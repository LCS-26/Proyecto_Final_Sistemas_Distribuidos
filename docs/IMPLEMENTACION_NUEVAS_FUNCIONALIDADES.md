# Implementación de Nuevas Funcionalidades — Stark Industries Security System


---

## Índice

1. [Resumen de cambios](#1-resumen-de-cambios)
2. [Infraestructura nueva](#2-infraestructura-nueva)
3. [Spring Cloud Netflix Eureka](#3-spring-cloud-netflix-eureka)
4. [RabbitMQ como orquestador](#4-rabbitmq-como-orquestador)
5. [Spring Data JPA y persistencia](#5-spring-data-jpa-y-persistencia)
6. [@Primary y @Qualifier](#6-primary-y-qualifier)
7. [@Scope Prototype + @PostConstruct + @PreDestroy](#7-scope-prototype--postconstruct--predestroy)
8. [ExecutorService y @Async](#8-executorservice-y-async)
9. [Bugs corregidos](#9-bugs-corregidos)
10. [Compatibilidad con tests E2E](#10-compatibilidad-con-tests-e2e)
11. [Frontend — Dashboard HTML](#11-frontend--dashboard-html)
12. [Cómo arrancar el sistema](#12-cómo-arrancar-el-sistema)
13. [Mapa de archivos nuevos](#13-mapa-de-archivos-nuevos)

---

## 1. Resumen de cambios

| Área | Antes | Después |
|------|-------|---------|
| Comunicación sensores→alert | HTTP REST directo (`AlertClient`) | **RabbitMQ AMQP** como canal principal + HTTP como fallback |
| Persistencia de alertas | `CopyOnWriteArrayList` en memoria | **Spring Data JPA + H2** con `@Transactional` |
| Descubrimiento de servicios | URLs hardcodeadas en variables de entorno | **Eureka Server** + Eureka Client en cada servicio |
| `application.properties` | Solo `application.yml` (puerto, nombre, actuator) | **`application.properties` añadido** en cada microservicio con config Eureka + RabbitMQ (ambos ficheros coexisten; requerimiento del profesor) |
| `@Async` en temperature | AUSENTE (bug) | `@EnableAsync` + `TemperatureAsyncAlertService` |
| Ambitos de beans | Solo Singleton implícito | `@Scope("prototype")` demostrado con `AlertEventBuilder` |
| Ciclo de vida de beans | Sin callbacks explícitos | `@PostConstruct` y `@PreDestroy` en `RabbitMQConfig` y `AlertEventBuilder` |
| Pool de hilos | Sin configuración explícita | `ThreadPoolTaskExecutor` configurado en `AsyncConfig` por servicio |
| `@Primary` / `@Qualifier` | Ausente | `AmqpAlertPublisher` (@Primary) vs `HttpAlertPublisher` (@Qualifier) |

---

## 2. Infraestructura nueva

### 2.1 Root `pom.xml`

Se añadió el BOM de Spring Cloud y el módulo `eureka-server`:

```xml
<properties>
   <spring-cloud.version>2024.0.1</spring-cloud.version>
</properties>

<modules>
<module>eureka-server</module>   <!-- NUEVO -->
...
</modules>

<dependencyManagement>
<dependencies>
   <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>${spring-cloud.version}</version>
      <type>pom</type>
      <scope>import</scope>
   </dependency>
</dependencies>
</dependencyManagement>
```

### 2.2 `docker-compose.yml`

Servicios nuevos añadidos:

```
rabbitmq      → puertos 5672 (AMQP) y 15672 (Management UI)
eureka-server → puerto 8761
```

Los servicios existentes ahora esperan a que RabbitMQ y Eureka estén sanos antes de arrancar (`healthcheck` + `depends_on: condition: service_healthy`).

---

## 3. Spring Cloud Netflix Eureka

**Concepto de apuntes:** Spring Cloud — Eureka Server como Service Registry

### 3.1 Módulo nuevo: `eureka-server`

```
eureka-server/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/.../eureka/EurekaServerApplication.java
    └── resources/application.properties
```

**`EurekaServerApplication.java`** — `@EnableEurekaServer` activa el servidor de registro:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
   public static void main(String[] args) {
      SpringApplication.run(EurekaServerApplication.class, args);
   }
}
```

**`application.properties`** (puerto 8761):

```properties
server.port=8761
spring.application.name=eureka-server
eureka.client.register-with-eureka=false   # el servidor no se registra a sí mismo
eureka.client.fetch-registry=false
```

Dashboard disponible en: **http://localhost:8761**

### 3.2 Eureka Client en cada microservicio

Todos los servicios tienen ahora `spring-cloud-starter-netflix-eureka-client` en su `pom.xml` y la siguiente configuración en su `application.properties`:

```properties
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true
spring.cloud.compatibility-verifier.enabled=false
```

---

## 4. RabbitMQ como orquestador

**Concepto de apuntes:** Spring AMQP / Mensajería asíncrona entre microservicios

### 4.1 Topología del broker

```
Sensor service
    │
    ▼  AmqpAlertPublisher.publish()
RabbitTemplate.convertAndSend()
    │
    ▼
DirectExchange: "stark.alerts.exchange"
    │  routing key: "stark.alert.critical"
    ▼
Queue: "stark.alerts.queue"  (durable)
    │
    ▼  @RabbitListener
AlertConsumer.receiveAlert()
    │
    ├─▶ AlertService.save()              → H2 (JPA)
    └─▶ AlertNotificationService.publish() → WebSocket /topic/alerts
```

### 4.2 Productor: `AmqpAlertPublisher` (en cada sensor service)

```java
@Primary
@Component
@ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
public class AmqpAlertPublisher implements AlertPublisher {

   private final RabbitTemplate rabbitTemplate;
   private final AlertPublisher httpFallback;   // @Qualifier("httpAlertPublisher")

   @Override
   public void publish(String source, double value, String message, String correlationId) {
      try {
         rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY,
                 new AlertAmqpMessage("HIGH", message, "MOVEMENT", source, value, Instant.now(), correlationId));
      } catch (Exception ex) {
         // Si RabbitMQ no está disponible → fallback HTTP
         httpFallback.publish(source, value, message, correlationId);
      }
   }
}
```

- El JSON se serializa con `Jackson2JsonMessageConverter` configurado en `RabbitMQConfig`
- Si el broker no está disponible, se hace fallback automático a HTTP (resiliencia)

### 4.3 Consumidor: `AlertConsumer` (alert-service)

```java
@Service
@ConditionalOnProperty(name = "stark.amqp.enabled", havingValue = "true", matchIfMissing = true)
public class AlertConsumer {

   @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
   public void receiveAlert(AlertAmqpMessage amqpMessage) {
      AlertRequest request = new AlertRequest(/* ... */);
      alertService.save(request, amqpMessage.correlationId());       // → JPA
      alertNotificationService.publish(request, amqpMessage.correlationId()); // → WebSocket
   }
}
```

### 4.4 `RabbitMQConfig` en alert-service — `@PostConstruct` y `@PreDestroy`

```java
@Configuration
public class RabbitMQConfig {

   @Bean @ConditionalOnProperty(...)
   public Queue alertQueue() { return new Queue(QUEUE_NAME, true); }

   @Bean @ConditionalOnProperty(...)
   public DirectExchange alertExchange() { return new DirectExchange(EXCHANGE_NAME); }

   @Bean @ConditionalOnProperty(...)
   public Binding alertBinding(...) { return BindingBuilder.bind(alertQueue).to(alertExchange).with(ROUTING_KEY); }

   @PostConstruct
   public void onStartup() {
      LOGGER.info("Sistema de mensajería RabbitMQ configurado y listo");
   }

   @PreDestroy
   public void onShutdown() {
      LOGGER.info("Cerrando sistema de mensajería RabbitMQ");
   }
}
```

**Concepto:** `@PostConstruct` se ejecuta UNA VEZ tras la inyección de dependencias. `@PreDestroy` se ejecuta antes de que el contenedor destruya el bean.

### 4.5 Acceso al Management UI de RabbitMQ

URL: **http://localhost:15672** — usuario: `guest` / contraseña: `guest`

Desde allí se pueden ver las colas, mensajes publicados, consumidores conectados, etc.

---

## 5. Spring Data JPA y persistencia

**Conceptos de apuntes:** Spring Data JPA, JDBC, @Transactional, Connection Pooling (HikariCP)

### 5.1 Entidad `Alert`

```java
@Entity
@Table(name = "alerts")
public class Alert {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   private String severity;
   private String message;
   private String sensorType;
   private String source;
   private double value;
   private Instant timestamp;
   private String correlationId;
}
```

Archivo: `alert-service/src/main/java/.../alert/model/Alert.java`

### 5.2 `AlertRepository`

```java
public interface AlertRepository extends JpaRepository<Alert, Long> {
   List<Alert> findAllByOrderByTimestampDesc();
}
```

Spring Data genera la implementación automáticamente. Solo se declara la interfaz.

Archivo: `alert-service/src/main/java/.../alert/repository/AlertRepository.java`

### 5.3 `AlertService` con `@Transactional`

```java
@Service
public class AlertService {

   @Transactional           // escritura: transacción completa con rollback automático si falla
   public Alert save(AlertRequest request, String correlationId) { ... }

   @Transactional(readOnly = true)  // lectura: optimización para queries de solo lectura
   public List<AlertRequest> findAll() { ... }
}
```

Archivo: `alert-service/src/main/java/.../alert/service/AlertService.java`

### 5.4 Configuración H2 (en `application.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:alertdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- Base de datos en memoria (H2) — no requiere instalación
- **HikariCP** (connection pooling) es configurado automáticamente por Spring Boot
- Consola H2 accesible en: **http://localhost:8090/h2-console**

### 5.5 `AlertController` actualizado

```java
// Antes: CopyOnWriteArrayList<AlertRequest> alerts = new CopyOnWriteArrayList<>()
// Ahora:
public class AlertController {
   private final AlertService alertService;  // ← Spring Data JPA

   @PostMapping("/internal/alerts")
   public void receiveAlert(@RequestBody AlertRequest request, ...) {
      alertService.save(request, cid);          // persiste en H2
      alertNotificationService.publish(request, cid);  // WebSocket
   }

   @GetMapping("/api/alerts")
   public List<AlertRequest> alerts(...) {
      return alertService.findAll();  // lee de H2, ordenado por fecha desc
   }
}
```

---

## 6. @Primary y @Qualifier

**Concepto de apuntes:** Resolución de conflictos cuando hay múltiples beans del mismo tipo

Cada sensor service tiene ahora dos implementaciones de `AlertPublisher`:

```
AlertPublisher (interfaz)
    ├── AmqpAlertPublisher  → @Primary    (publicación via RabbitMQ)
    └── HttpAlertPublisher  → @Qualifier("httpAlertPublisher")  (publicación via HTTP REST)
```

### Cómo Spring resuelve la inyección

```java
// En MovementAsyncAlertService — Spring inyecta @Primary automáticamente:
private final AlertPublisher alertPublisher;  // → AmqpAlertPublisher

// Para inyectar específicamente el HTTP:
@Qualifier("httpAlertPublisher")
private final AlertPublisher alertPublisher;  // → HttpAlertPublisher
```

### Dentro de `AmqpAlertPublisher` — uso de @Qualifier para el fallback:

```java
public AmqpAlertPublisher(
        RabbitTemplate rabbitTemplate,
        @Qualifier("httpAlertPublisher") AlertPublisher httpFallback) {
   // httpFallback = HttpAlertPublisher (seleccionado por qualifier, no por @Primary)
}
```

---

## 7. @Scope Prototype + @PostConstruct + @PreDestroy

**Concepto de apuntes:** Ámbitos de beans (Singleton vs Prototype) y ciclo de vida

### `AlertEventBuilder` en cada sensor service

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)  // nueva instancia cada vez que se pide al contenedor
public class AlertEventBuilder {

   @PostConstruct
   public void init() {
      // Se llama DESPUÉS de que Spring inyecta dependencias
      // En Prototype: ocurre con CADA nueva instancia
      LOGGER.debug("Nueva instancia de AlertEventBuilder creada (Prototype scope)");
   }

   @PreDestroy
   public void destroy() {
      // Se llama antes de destrucción del bean
      // NOTA: Spring NO llama @PreDestroy en Prototype — el caller gestiona el ciclo de vida
      LOGGER.debug("AlertEventBuilder siendo destruido");
   }

   // Fluent API
   public AlertEventBuilder withSensorType(String sensorType) { ... }
   public AlertEventBuilder withSource(String source) { ... }
   public AlertEventBuilder withValue(double value) { ... }
}
```

### Contraste Singleton vs Prototype

| | Singleton (defecto) | Prototype (@Scope) |
|---|---|---|
| Instancias | Una única por contenedor | Nueva por cada solicitud |
| `@PostConstruct` | Una vez al arrancar | Una vez por cada instancia creada |
| `@PreDestroy` | Al cerrar el contexto | **No gestionado por Spring** |
| Uso típico | Servicios, repositorios | Objetos con estado, builders |

---

## 8. ExecutorService y @Async

**Concepto de apuntes:** Programación concurrente, ExecutorService, pool de hilos

### `AsyncConfig` en cada sensor service

```java
@Configuration
public class AsyncConfig implements AsyncConfigurer {

   @Bean(name = "taskExecutor")
   public Executor taskExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);      // mínimo 2 hilos siempre activos
      executor.setMaxPoolSize(10);      // hasta 10 hilos si la cola está llena
      executor.setQueueCapacity(100);   // tareas en espera antes de crear más hilos
      executor.setThreadNamePrefix("stark-movement-async-");
      executor.setWaitForTasksToCompleteOnShutdown(true);
      executor.initialize();
      return executor;
   }

   @Override
   public Executor getAsyncExecutor() {
      return taskExecutor();
   }
}
```

### Flujo @Async completo

```
HTTP Request (hilo-1) ──► Controller
                              │
                              ├─ crea SensorEventResponse (síncrono, responde 202 inmediatamente)
                              │
                              └─ asyncAlertService.sendCriticalAlert() ──► @Async
                                        (hilo del pool: stark-movement-async-1)
                                              │
                                              └─ alertPublisher.publish()
                                                    ├─ AmqpAlertPublisher → RabbitMQ
                                                    └─ HttpAlertPublisher → HTTP (fallback)
```

El hilo HTTP no espera a que se envíe la alerta. Responde inmediatamente con `202 Accepted`.

---

## 9. Bugs corregidos

### 9.1 `TemperatureController` — llamada síncrona (bug grave)

**Antes:**
```java
// El hilo HTTP queda BLOQUEADO esperando la respuesta del alert-service
if (critical) {
        alertClient.sendCriticalAlert(source, value, "Overheat detected", cid);
}
```

**Después:**
```java
// El hilo HTTP responde inmediatamente; la alerta se envía en otro hilo
if (critical) {
        temperatureAsyncAlertService.sendCriticalAlert(request, cid);  // @Async
}
```

Archivo corregido: `sensor-temperature-service/.../api/TemperatureController.java`
Archivo nuevo: `sensor-temperature-service/.../service/TemperatureAsyncAlertService.java`

### 9.2 `AccessController` — llamada síncrona (bug grave)

Mismo problema que temperature. Ahora usa `AccessAsyncAlertService` (@Async).

Archivo corregido: `sensor-access-service/.../api/AccessController.java`
Archivo nuevo: `sensor-access-service/.../service/AccessAsyncAlertService.java`

### 9.3 `SensorTemperatureApplication` — faltaba `@EnableAsync`

**Antes:**
```java
@SpringBootApplication
// @EnableAsync AUSENTE — los métodos @Async se ejecutaban de forma síncrona en modo debug
public class SensorTemperatureApplication { ... }
```

**Después:**
```java
@SpringBootApplication
@EnableAsync  // ahora los @Async se ejecutan en el ThreadPoolTaskExecutor
public class SensorTemperatureApplication { ... }
```

### 9.4 Servicios `@Async` inyectaban `AlertClient` directamente

`MovementAsyncAlertService`, `SeismicAsyncAlertService`, `DoorOpenAsyncAlertService` inyectaban `AlertClient` directamente. Ahora inyectan `AlertPublisher` (interfaz), que Spring resuelve a `AmqpAlertPublisher` (@Primary) o `HttpAlertPublisher` según disponibilidad.

---

## 10. Compatibilidad con tests E2E

Los tests E2E (`MicroservicesE2ETest`) arrancan todos los servicios en la misma JVM sin RabbitMQ ni Eureka. Se mantiene compatibilidad así:

### Mecanismo

1. **`@ConditionalOnProperty(stark.amqp.enabled, matchIfMissing = true)`** en:
   - `AmqpAlertPublisher` (los 3 sensor services)
   - `RabbitMQConfig` (los 3 sensor services)
   - `AlertConsumer`, `Queue`/`Exchange`/`Binding` beans (alert-service)

2. Los tests arrancan con: `--stark.amqp.enabled=false` + `--spring.autoconfigure.exclude=...RabbitAutoConfiguration`

3. Cuando `stark.amqp.enabled=false`:
   - `AmqpAlertPublisher` NO se crea → `HttpAlertPublisher` queda como único `AlertPublisher`
   - Los sensores envían alertas via HTTP directamente al alert-service
   - `AlertConsumer` NO se crea → sin conexión a RabbitMQ
   - Las alertas se guardan en H2 vía `AlertService`
   - Los tests siguen pasando ✓

### Tests unitarios por servicio

Cada servicio tiene `src/test/resources/application.properties` con:

```properties
stark.amqp.enabled=false
eureka.client.enabled=false
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

---

## 11. Frontend — Dashboard HTML

### Descripción

Se añadió un dashboard web completo en HTML puro + CSS + JS vanilla, servido por el propio `gateway-service` como recurso estático.

Archivo: `gateway-service/src/main/resources/static/index.html`

### Funcionalidades

| Funcionalidad | Tecnología |
|---|---|
| Pantalla de login | Formulario HTML → guarda credenciales en memoria JS |
| Alertas en tiempo real | WebSocket + STOMP (`@stomp/stompjs` CDN) → `ws://localhost:8090/ws/alerts` |
| Historial de alertas | `GET /api/alerts` con Basic Auth, auto-refresh cada 15s |
| Simulador de sensores | Formulario con presets → `POST /api/{sensor}/events` con Basic Auth |
| Notificaciones toast | Se muestran al recibir nueva alerta via WebSocket |
| Rol-based UI | SENSOR no ve historial; OPERATOR/ADMIN ven todo |

### Tema visual

Estilo oscuro inspirado en Stark Industries, con acentos dorados (`#ffd700`), tipografía monospace y tarjetas de estadísticas en tiempo real.

### Cambio en `SecurityConfig.java`

Para que el navegador pueda cargar `index.html` sin pedir credenciales (el login es propio del HTML), se añadió la regla `permitAll()` para los recursos estáticos:

```java
// Antes: anyRequest().authenticated() bloqueaba index.html
// Después:
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()  // ← NUEVO
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/api/movement/**", ...).hasAnyRole("SENSOR", "OPERATOR", "ADMIN")
    .requestMatchers("/api/access/check", "/api/alerts").hasAnyRole("OPERATOR", "ADMIN")
    .anyRequest().authenticated())
```

Archivo: `gateway-service/src/main/java/.../config/SecurityConfig.java`

**Motivo:** Sin este cambio, el navegador recibe un `401 Unauthorized` antes de poder mostrar la pantalla de login, ya que HTTP Basic Auth no tiene una "pantalla de login" propia que el HTML pueda interceptar a nivel de navegador.

### Flujo de autenticación del frontend

```
1. Browser → GET http://localhost:8080/         → 200 OK (sin auth, permitAll)
2. Usuario introduce credenciales en el formulario HTML
3. JS guarda { username, password } en memoria (no en localStorage por seguridad)
4. Cada llamada a /api/** incluye:
   Authorization: Basic base64(username:password)
5. WebSocket se conecta a ws://localhost:8090/ws/alerts (cross-origin, ya permitido por WebSocketConfig)
```

---

## 12. Cómo arrancar el sistema

### Acceso al Dashboard

Una vez arrancado, abrir en el navegador: **http://localhost:8080**

Credenciales disponibles:

| Usuario | Contraseña | Rol | Acceso |
|---|---|---|---|
| `operator` | `operator-pass` | OPERATOR | Dashboard + historial |
| `admin` | `admin-pass` | ADMIN | Dashboard + historial |
| `sensor-node` | `sensor-pass` | SENSOR | Solo envío de lecturas |

### Con Docker Compose (recomendado)

```bash
docker-compose up --build
```

Orden de arranque automático: `rabbitmq` → `eureka-server` → `alert-service` → sensores → `gateway-service`

### Servicios y URLs

| Servicio | URL | Descripción |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | Ver microservicios registrados |
| RabbitMQ UI | http://localhost:15672 | Ver colas y mensajes (guest/guest) |
| Gateway | http://localhost:8080 | Punto de entrada principal |
| Alert Service | http://localhost:8090 | Alertas + WebSocket + H2 Console |
| H2 Console | http://localhost:8090/h2-console | Base de datos en memoria |
| Movement Sensor | http://localhost:8081 | Sensor de movimiento |
| Temperature Sensor | http://localhost:8082 | Sensor de temperatura |
| Access Sensor | http://localhost:8083 | Sensor de acceso |

### Probar el flujo RabbitMQ

```bash
# 1. Enviar lectura crítica de temperatura (≥ 60°C) → genera alerta via RabbitMQ
curl -X POST http://localhost:8080/api/temperature/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'sensor-node:sensor-pass' | base64)" \
  -d '{"source":"sensor-sala","value":75.0,"details":"Sala de servidores"}'

# 2. Ver alertas guardadas en H2 (via gateway)
curl http://localhost:8080/api/alerts \
  -H "Authorization: Basic $(echo -n 'operator:operator-pass' | base64)"

# 3. Ver la alerta en RabbitMQ UI → http://localhost:15672
#    Queues → stark.alerts.queue → Get messages
```

---

## 13. Mapa de archivos nuevos

### Archivos NUEVOS creados

```
eureka-server/                                        ← MÓDULO NUEVO
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/org/toolset/grupo1/eureka/
    │   └── EurekaServerApplication.java              ← @EnableEurekaServer
    └── resources/
        └── application.properties

alert-service/src/main/java/.../alert/
├── model/
│   └── Alert.java                                    ← @Entity JPA
├── repository/
│   └── AlertRepository.java                          ← JpaRepository
├── service/
│   └── AlertService.java                             ← @Transactional
├── config/
│   └── RabbitMQConfig.java                          ← REEMPLAZA el anterior, añade @PostConstruct/@PreDestroy
└── messaging/
    ├── AlertAmqpMessage.java                         ← DTO para mensajes RabbitMQ
    └── AlertConsumer.java                            ← @RabbitListener

sensor-movement-service/src/main/java/.../movement/
├── publisher/
│   ├── AlertPublisher.java                           ← INTERFAZ nueva
│   ├── AmqpAlertPublisher.java                       ← @Primary (RabbitMQ)
│   └── HttpAlertPublisher.java                       ← @Qualifier (HTTP fallback)
└── config/
    ├── AsyncConfig.java                              ← ThreadPoolTaskExecutor
    ├── RabbitMQConfig.java                           ← Exchange + routing key
    └── AlertEventBuilder.java                        ← @Scope("prototype")

sensor-temperature-service/src/main/java/.../temperature/
├── publisher/                                        ← mismo patrón que movement
│   ├── AlertPublisher.java
│   ├── AmqpAlertPublisher.java
│   └── HttpAlertPublisher.java
├── config/
│   ├── AsyncConfig.java
│   ├── RabbitMQConfig.java
│   └── AlertEventBuilder.java
└── service/
    └── TemperatureAsyncAlertService.java             ← NUEVO (bug fix)

sensor-access-service/src/main/java/.../access/
├── publisher/                                        ← mismo patrón que movement
│   ├── AlertPublisher.java
│   ├── AmqpAlertPublisher.java
│   └── HttpAlertPublisher.java
├── config/
│   ├── AsyncConfig.java
│   ├── RabbitMQConfig.java
│   └── AlertEventBuilder.java
└── service/
    └── AccessAsyncAlertService.java                  ← NUEVO (bug fix)
```

### Archivos MODIFICADOS

```
pom.xml                                               ← Spring Cloud BOM + módulo eureka-server
docker-compose.yml                                    ← RabbitMQ + eureka-server + healthchecks
alert-service/pom.xml                                 ← amqp + data-jpa + h2 + eureka-client
alert-service/.../api/AlertController.java            ← usa AlertService en vez de lista en memoria
sensor-movement-service/pom.xml                       ← amqp + eureka-client
sensor-movement-service/.../service/MovementAsyncAlertService.java  ← inyecta AlertPublisher
sensor-movement-service/.../seismic/service/SeismicAsyncAlertService.java ← inyecta AlertPublisher
sensor-temperature-service/pom.xml                    ← amqp + eureka-client
sensor-temperature-service/SensorTemperatureApplication.java  ← añadido @EnableAsync
sensor-temperature-service/.../api/TemperatureController.java ← usa TemperatureAsyncAlertService
sensor-access-service/pom.xml                         ← amqp + eureka-client
sensor-access-service/.../api/AccessController.java   ← usa AccessAsyncAlertService
sensor-access-service/.../service/DoorOpenAsyncAlertService.java ← inyecta AlertPublisher
gateway-service/pom.xml                               ← eureka-client
gateway-service/.../config/SecurityConfig.java        ← permitAll() para / e /index.html
gateway-service/.../e2e/MicroservicesE2ETest.java     ← desactiva AMQP y Eureka en tests
```

### Archivos `application.properties` NUEVOS (requerimiento del profesor)

```
eureka-server/src/main/resources/application.properties
alert-service/src/main/resources/application.properties
sensor-movement-service/src/main/resources/application.properties
sensor-temperature-service/src/main/resources/application.properties
sensor-access-service/src/main/resources/application.properties
gateway-service/src/main/resources/application.properties
```

### Archivos `application.properties` de TEST NUEVOS

```
alert-service/src/test/resources/application.properties
sensor-movement-service/src/test/resources/application.properties
sensor-temperature-service/src/test/resources/application.properties
sensor-access-service/src/test/resources/application.properties
gateway-service/src/test/resources/application.properties
```

### Frontend NUEVO

```
gateway-service/src/main/resources/static/
└── index.html                                        ← Dashboard HTML+CSS+JS
```
