# Análisis Completo del Proyecto — Stark Industries Security System

> **Última actualización:** Abril 2026 — refleja el estado actual del código.

---

## 1. Estructura General del Proyecto

Proyecto **Maven multi-módulo** con Spring Boot **3.4.5**, Spring Cloud **2024.0.1** y Java 21.

```
stark-security-microservices/          ← pom.xml raíz (packaging=pom)
├── eureka-server/                     ← Puerto 8761  (Service Registry)
├── alert-service/                     ← Puerto 8090
├── gateway-service/                   ← Puerto 8080
├── sensor-movement-service/           ← Puerto 8081
├── sensor-temperature-service/        ← Puerto 8082
├── sensor-access-service/             ← Puerto 8083
├── docker-compose.yml
└── docs/
    ├── statement-mapping.md
    ├── performance-benchmark.md
    ├── IMPLEMENTACION_NUEVAS_FUNCIONALIDADES.md
    └── ANALISIS_PROYECTO.md           ← (este archivo)
```

---

## 2. Descripción de Cada Microservicio

### 2.1 `eureka-server` (puerto 8761)

**Responsabilidad:** Registro centralizado de microservicios (Service Registry). Permite que cada servicio se registre y descubra a los demás de forma dinámica.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `EurekaServerApplication.java` | `@SpringBootApplication` + `@EnableEurekaServer` |
| `application.properties` | Puerto 8761, `register-with-eureka=false`, `fetch-registry=false` |

**Concepto cubierto:** Spring Cloud Netflix Eureka Server como Service Registry.

**Dashboard disponible en:** http://localhost:8761

---

### 2.2 `alert-service` (puerto 8090)

**Responsabilidad:** Consumir alertas de los sensores (vía RabbitMQ como canal principal, HTTP como fallback), persistirlas en base de datos y retransmitirlas en tiempo real por WebSocket.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `AlertServiceApplication.java` | `@SpringBootApplication` — punto de entrada |
| `AlertController.java` | `POST /internal/alerts` (fallback HTTP) · `GET /api/alerts` |
| `AlertRequest.java` | Record con severity, message, sensorType, source, value, timestamp |
| `AlertNotificationService.java` | `@Service` — publica en `/topic/alerts` via `SimpMessagingTemplate` |
| `AlertMessage.java` | Record para la payload del WebSocket |
| `WebSocketConfig.java` | `@EnableWebSocketMessageBroker` — STOMP en `/ws/alerts`, broker en `/topic` |
| `InternalSecurityConfig.java` | `WebMvcConfigurer` — registra el interceptor en `/internal/**` |
| `InternalTokenInterceptor.java` | `HandlerInterceptor` — valida cabecera `X-Internal-Token` (401 si falla) |
| `RabbitMQConfig.java` | `Queue`, `DirectExchange`, `Binding` + `@PostConstruct`/`@PreDestroy` |
| `AlertConsumer.java` | `@RabbitListener` — consume mensajes de RabbitMQ y dispara JPA + WebSocket |
| `AlertAmqpMessage.java` | Record DTO para deserializar mensajes AMQP (JSON → Java) |
| `Alert.java` | Entidad `@Entity` JPA — tabla `alerts` en H2 |
| `AlertRepository.java` | `JpaRepository<Alert, Long>` — `findAllByOrderByTimestampDesc()` |
| `AlertService.java` | `@Transactional` — `save()` + `findAll()` con Spring Data JPA |

**Almacenamiento:** Spring Data JPA + H2 en memoria. `AlertService` con `@Transactional` garantiza atomicidad. HikariCP gestiona el connection pool automáticamente.

**Camino principal de alertas:** RabbitMQ → `AlertConsumer` → `AlertService.save()` + `AlertNotificationService.publish()`.

**Fallback HTTP:** `POST /internal/alerts` protegido por `X-Internal-Token`. Actúa cuando RabbitMQ no está disponible.

**Consola H2:** http://localhost:8090/h2-console (JDBC URL: `jdbc:h2:mem:alertdb`)

**Monitoring:** Spring Actuator expone `health`, `info`, `metrics`.

**Tests:**
- `AlertServiceApplicationTests` — context load
- `InternalAlertSecurityTest` — valida 401 sin token y 202 con token correcto

---

### 2.3 `gateway-service` (puerto 8080)

**Responsabilidad:** Puerta de entrada única para clientes externos. Autentica, autoriza y enruta peticiones a los microservicios internos. Sirve el dashboard HTML.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `GatewayServiceApplication.java` | `@SpringBootApplication` — punto de entrada |
| `SecurityConfig.java` | Spring Security HTTP Basic, usuarios en memoria, roles, `permitAll` para `/index.html` |
| `GatewayController.java` | `@RestController` — proxy/router hacia los 4 servicios via `RestClient` |
| `static/index.html` | Dashboard HTML+CSS+JS — login, alertas WebSocket, simulador de sensores |

**Usuarios y roles (en memoria):**

| Usuario | Contraseña | Rol |
|---|---|---|
| `sensor-node` | `sensor-pass` | `ROLE_SENSOR` |
| `operator` | `operator-pass` | `ROLE_OPERATOR` |
| `admin` | `admin-pass` | `ROLE_ADMIN` |

**Endpoints expuestos:**

| Método | Path | Destino interno | Roles permitidos |
|---|---|---|---|
| `GET` | `/` · `/index.html` | (estático) | Público |
| `POST/GET` | `/api/movement/events` | `sensor-movement-service:8081/api/events` | SENSOR, OPERATOR, ADMIN |
| `POST/GET` | `/api/seismic/events` | `sensor-movement-service:8081/api/seismic/events` | SENSOR, OPERATOR, ADMIN |
| `POST/GET` | `/api/temperature/events` | `sensor-temperature-service:8082/api/events` | SENSOR, OPERATOR, ADMIN |
| `POST/GET` | `/api/access/events` | `sensor-access-service:8083/api/events` | SENSOR, OPERATOR, ADMIN |
| `POST/GET` | `/api/door-open/events` | `sensor-access-service:8083/api/door-open/events` | SENSOR, OPERATOR, ADMIN |
| `POST` | `/api/access/check` | `sensor-access-service:8083/api/access/check` | OPERATOR, ADMIN |
| `GET` | `/api/alerts` | `alert-service:8090/api/alerts` | OPERATOR, ADMIN |

**Características:**
- Propaga `X-Correlation-Id` (genera UUID si no viene en la petición)
- Usa `RestClient` (API fluida de Spring Boot 3.x) para llamadas HTTP salientes
- URLs de destino configurables via variables de entorno (`services.movement-url`, etc.)
- Servicio registrado en Eureka (`spring-cloud-starter-netflix-eureka-client`)

**Dependencias test-scope:** `alert-service`, `sensor-movement-service`, `sensor-temperature-service`, `sensor-access-service` (para levantar todos en `MicroservicesE2ETest`).

**Tests:**
- `GatewayServiceApplicationTests` — context load
- `MicroservicesE2ETest` — inicia los 5 servicios en puertos aleatorios y valida flujos E2E:
  - Movimiento crítico (`value ≥ 1.0`) → alerta HIGH generada y persistida
  - Temperatura normal (`value < 60.0`) → sin alerta crítica
  - Acceso denegado (`value ≤ 0.0`) → alerta HIGH generada y persistida

---

### 2.4 `sensor-movement-service` (puerto 8081)

**Responsabilidad:** Procesar eventos de sensores de movimiento y sísmicos, publicar alertas críticas vía RabbitMQ.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `SensorMovementApplication.java` | `@SpringBootApplication` + `@EnableAsync` |
| `MovementController.java` | `POST/GET /api/events` — delega en `MovementDetectionService` |
| `MovementDetectionService.java` | `@Service` — lógica: crítico si `value >= 1.0`; dispara `MovementAsyncAlertService` |
| `MovementAsyncAlertService.java` | `@Async` — publica alerta vía `AlertPublisher` (@Primary) |
| `SeismicController.java` | `POST/GET /api/seismic/events` |
| `SeismicDoorService.java` | Lógica: crítico si `impactForce >= 7.0` |
| `SeismicAsyncAlertService.java` | `@Async` — publica alerta vía `AlertPublisher` (@Primary) |
| `AlertClient.java` | Cliente HTTP a `alert-service /internal/alerts` (usado por `HttpAlertPublisher`) |
| `AlertPublisher.java` | **Interfaz** — abstracción del canal de publicación |
| `AmqpAlertPublisher.java` | `@Primary` + `@ConditionalOnProperty` — publica vía `RabbitTemplate` |
| `HttpAlertPublisher.java` | `@Qualifier("httpAlertPublisher")` — fallback HTTP vía `AlertClient` |
| `AsyncConfig.java` | `ThreadPoolTaskExecutor` (core=2, max=10, queue=100) · prefijo `stark-movement-async-` |
| `RabbitMQConfig.java` | `@ConditionalOnProperty` — constantes `EXCHANGE_NAME` y `ROUTING_KEY` + `RabbitTemplate` |
| `AlertEventBuilder.java` | `@Scope("prototype")` + `@PostConstruct` + `@PreDestroy` — builder de alertas |

**Umbrales de criticidad:**
- Movimiento: `value >= 1.0`
- Sísmico: `impactForce >= 7.0`

**Thread-safety:** `CopyOnWriteArrayList` para el histórico de eventos en memoria.

**Tests:** `SensorMovementApplicationTests` — context load (AMQP y Eureka desactivados en `src/test/resources/application.properties`).

---

### 2.5 `sensor-temperature-service` (puerto 8082)

**Responsabilidad:** Procesar eventos de temperatura, publicar alertas críticas vía RabbitMQ.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `SensorTemperatureApplication.java` | `@SpringBootApplication` + **`@EnableAsync`** (corregido) |
| `TemperatureController.java` | `POST/GET /api/events` — crítico si `value >= 60.0`; llama `TemperatureAsyncAlertService` |
| `TemperatureAsyncAlertService.java` | `@Async` — publica alerta vía `AlertPublisher` (@Primary) |
| `AlertClient.java` | Cliente HTTP (usado por `HttpAlertPublisher`) |
| `AlertPublisher.java` | Interfaz de publicación |
| `AmqpAlertPublisher.java` | `@Primary` + `@ConditionalOnProperty` — RabbitMQ |
| `HttpAlertPublisher.java` | `@Qualifier("httpAlertPublisher")` — fallback HTTP |
| `AsyncConfig.java` | `ThreadPoolTaskExecutor` · prefijo `stark-temperature-async-` |
| `RabbitMQConfig.java` | `@ConditionalOnProperty` — exchange + routing key + `RabbitTemplate` |
| `AlertEventBuilder.java` | `@Scope("prototype")` + `@PostConstruct` + `@PreDestroy` |

**Umbral de criticidad:** `value >= 60.0` (grados Celsius).

**Thread-safety:** `CopyOnWriteArrayList` para el histórico de eventos.

---

### 2.6 `sensor-access-service` (puerto 8083)

**Responsabilidad:** Gestionar control de acceso y monitorizar puertas, publicar alertas críticas vía RabbitMQ.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `SensorAccessApplication.java` | `@SpringBootApplication` + `@EnableAsync` |
| `AccessController.java` | `POST/GET /api/events` — crítico si `value <= 0.0`; llama `AccessAsyncAlertService` |
| `AccessAsyncAlertService.java` | `@Async` — publica alerta de acceso vía `AlertPublisher` (@Primary) |
| `AccessPolicyController.java` | `POST /api/access/check` |
| `AccessControlService.java` | Lógica de autorización por badge y área |
| `DoorOpenController.java` | `POST/GET /api/door-open/events` |
| `DoorOpenService.java` | Lógica: crítico si la puerta está abierta |
| `DoorOpenAsyncAlertService.java` | `@Async` — publica alerta de puerta vía `AlertPublisher` (@Primary) |
| `AlertClient.java` | Cliente HTTP (usado por `HttpAlertPublisher`) |
| `AlertPublisher.java` | Interfaz de publicación |
| `AmqpAlertPublisher.java` | `@Primary` + `@ConditionalOnProperty` — RabbitMQ |
| `HttpAlertPublisher.java` | `@Qualifier("httpAlertPublisher")` — fallback HTTP |
| `AsyncConfig.java` | `ThreadPoolTaskExecutor` · prefijo `stark-access-async-` |
| `RabbitMQConfig.java` | `@ConditionalOnProperty` — exchange + routing key + `RabbitTemplate` |
| `AlertEventBuilder.java` | `@Scope("prototype")` + `@PostConstruct` + `@PreDestroy` |

**Lógica de control de acceso (`AccessControlService`):**
1. `!badgeValid` → denegado: "Invalid badge"
2. `area.contains("vault") && !username.startsWith("stark")` → denegado: "Restricted area policy"
3. Pasa ambas validaciones → concedido: "Access granted"

**Thread-safety:** `CopyOnWriteArrayList` para el histórico de eventos.

---

## 3. Arquitectura de Comunicación Actual

```
[Cliente externo]
      │  HTTP Basic Auth
      ▼
[gateway-service :8080]   ← Spring Security (SENSOR/OPERATOR/ADMIN)
  └── static/index.html   ← Dashboard HTML+WebSocket
      │
      │ HTTP REST (RestClient) — síncrono / X-Internal-Token + X-Correlation-Id
      ├──────────────────────────────────────────────────────────────┐
      ▼                    ▼                    ▼                    ▼
[movement :8081]   [temperature :8082]   [access :8083]   [alert-service :8090]
      │ @Async              │ @Async              │ @Async
      └────────────────────┬┘────────────────────┘
                           │  AMQP (canal principal)
                           ▼
                   [RabbitMQ :5672]
                   Exchange: stark.alerts.exchange (DirectExchange)
                   Routing key: stark.alert.critical
                   Queue: stark.alerts.queue (durable)
                           │
                           │  @RabbitListener
                           ▼
                   [alert-service :8090]
                       │           │
                       │ JPA       │ STOMP/WebSocket
                       ▼           ▼
               [H2 :mem/alertdb]  [clientes WebSocket]
                                  (/topic/alerts)

[eureka-server :8761]  ← Todos los servicios se registran aquí
```

**Flujo de alerta crítica:**
1. Sensor detecta evento crítico (p. ej. temperatura ≥ 60 °C)
2. Controller llama al `*AsyncAlertService` correspondiente (`@Async` → hilo del pool)
3. El hilo HTTP responde `202 Accepted` inmediatamente
4. En paralelo, `AmqpAlertPublisher` serializa a JSON y publica en el exchange via `RabbitTemplate`
5. Si RabbitMQ falla → fallback a `HttpAlertPublisher` → `POST /internal/alerts`
6. `AlertConsumer` (@RabbitListener) recibe el mensaje en alert-service
7. `AlertService.save()` persiste en H2 con `@Transactional`
8. `AlertNotificationService.publish()` emite por WebSocket a `/topic/alerts`

---

## 4. Infraestructura Docker

**`docker-compose.yml`** define 7 servicios:

```
rabbitmq         → 5672 (AMQP) + 15672 (Management UI)  ← con healthcheck
eureka-server    → 8761:8761                              ← depende de rabbitmq
alert-service    → 8090:8090                              ← depende de rabbitmq (healthy) + eureka (healthy)
movement-service → 8081:8081                              ← depende de rabbitmq (healthy) + eureka (healthy)
temperature-service → 8082:8082                           ← depende de rabbitmq (healthy) + eureka (healthy)
access-service   → 8083:8083                              ← depende de rabbitmq (healthy) + eureka (healthy)
gateway-service  → 8080:8080                              ← depende de todos los anteriores
```

**Orden de arranque garantizado:** RabbitMQ → Eureka → alert-service + sensores → gateway.

**Variables de entorno relevantes:**
- `SPRING_RABBITMQ_HOST`: hostname del broker (valor: `rabbitmq` en Docker)
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: URL del Eureka server
- `INTERNAL_TOKEN`: token secreto para `/internal/alerts`
- `ALERT_SERVICE_URL`: URL del alert-service (para fallback HTTP en sensores)

**Acceso al Management UI de RabbitMQ:** http://localhost:15672 — usuario: `guest` / `guest`

---

## 5. Conceptos de Spring del Tema 1 — Inventario

| Concepto del Tema | ¿Implementado? | Dónde |
|---|:---:|---|
| `@SpringBootApplication` | ✅ | Todos los microservicios |
| IoC / Contenedor Spring | ✅ | Implícito en todo el proyecto |
| Inyección de dependencias (constructor injection) | ✅ | Todos los services y controllers |
| `@Service`, `@RestController`, `@Component` | ✅ | En todos los módulos |
| `@Repository` (Spring Data) | ✅ | `AlertRepository` — `JpaRepository<Alert, Long>` |
| `@Configuration` + `@Bean` | ✅ | `RabbitMQConfig`, `AsyncConfig`, `WebSocketConfig`, `SecurityConfig` |
| `@Async` + `@EnableAsync` | ✅ | Los 3 sensor services (movement, temperature, access) |
| `ExecutorService` / `ThreadPoolTaskExecutor` | ✅ | `AsyncConfig` en cada sensor service (core=2, max=10) |
| `@Scope("prototype")` | ✅ | `AlertEventBuilder` en cada sensor service |
| `@Primary` | ✅ | `AmqpAlertPublisher` en cada sensor service |
| `@Qualifier` | ✅ | `HttpAlertPublisher` (`@Qualifier("httpAlertPublisher")`) |
| `@PostConstruct` | ✅ | `RabbitMQConfig.onStartup()` en alert-service; `AlertEventBuilder.init()` en sensores |
| `@PreDestroy` | ✅ | `RabbitMQConfig.onShutdown()` en alert-service; `AlertEventBuilder.destroy()` en sensores |
| `@ConditionalOnProperty` | ✅ | `AmqpAlertPublisher`, `RabbitMQConfig`, `AlertConsumer` (activados con `stark.amqp.enabled=true`) |
| Spring Security | ✅ | `gateway-service` (HTTP Basic, roles, `InMemoryUserDetailsManager`) |
| WebSocket / STOMP | ✅ | `alert-service` — endpoint `/ws/alerts`, topic `/topic/alerts` |
| Spring Actuator | ✅ | Todos los servicios |
| Spring Data JPA | ✅ | `alert-service` — `AlertRepository`, `Alert` (@Entity), H2 |
| `@Transactional` | ✅ | `AlertService.save()` + `AlertService.findAll(readOnly=true)` |
| Connection Pooling (HikariCP) | ✅ | Configurado automáticamente por Spring Boot en alert-service |
| Spring AMQP / RabbitMQ | ✅ | `AmqpAlertPublisher` (productor) + `AlertConsumer` (consumidor) |
| Spring Cloud Netflix Eureka | ✅ | `eureka-server` + Eureka Client en los 5 servicios restantes |
| Logging estructurado | ✅ | Formato `event=xxx source=xxx cid=xxx key=value` en todos los servicios |
| Docker / fat jar | ✅ | Dockerfiles con multi-stage build en cada módulo |

---

## 6. Topología RabbitMQ

```
Sensor service
    │
    ▼  *AsyncAlertService.sendCriticalAlert()  [@Async → pool de hilos]
AmqpAlertPublisher.publish()
    │
    ▼  RabbitTemplate.convertAndSend()  [JSON via Jackson2JsonMessageConverter]
DirectExchange: "stark.alerts.exchange"
    │  routing key: "stark.alert.critical"
    ▼
Queue: "stark.alerts.queue"  (durable=true)
    │
    ▼  @RabbitListener
AlertConsumer.receiveAlert(AlertAmqpMessage)
    │
    ├─▶ AlertService.save()              → H2 (JPA, @Transactional)
    └─▶ AlertNotificationService.publish() → WebSocket /topic/alerts
```

**Resiliencia:** Si RabbitMQ no está disponible, `AmqpAlertPublisher` captura la excepción y hace fallback a `HttpAlertPublisher` (`@Qualifier("httpAlertPublisher")`), que llama a `POST /internal/alerts` directamente.

**Compatibilidad con tests:** `@ConditionalOnProperty(stark.amqp.enabled, matchIfMissing=true)` en `AmqpAlertPublisher`, `RabbitMQConfig` (sensores) y `AlertConsumer`, `Queue`, `Exchange`, `Binding` (alert-service). Los tests desactivan AMQP con `stark.amqp.enabled=false` y excluyen `RabbitAutoConfiguration`.

---

## 7. Configuración de `application.properties` por servicio

Cada módulo tiene dos ficheros:
- `src/main/resources/application.properties` — configuración en ejecución
- `src/test/resources/application.properties` — configuración para tests (sin RabbitMQ ni Eureka)

**Propiedades comunes a los sensores (main):**
```properties
spring.cloud.compatibility-verifier.enabled=false
eureka.client.service-url.defaultZone=${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
eureka.client.register-with-eureka=true
eureka.instance.prefer-ip-address=true
spring.rabbitmq.host=${SPRING_RABBITMQ_HOST:localhost}
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

**Propiedades de test (todos los servicios):**
```properties
stark.amqp.enabled=false
eureka.client.enabled=false
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

---

## 8. Frontend — Dashboard HTML

**Archivo:** `gateway-service/src/main/resources/static/index.html`

Servido por gateway-service como recurso estático (accesible sin autenticación gracias a `permitAll` en `SecurityConfig`).

| Funcionalidad | Tecnología |
|---|---|
| Pantalla de login | Formulario HTML → credenciales en memoria JS (no en localStorage) |
| Alertas en tiempo real | WebSocket + STOMP (`@stomp/stompjs` CDN) → `ws://localhost:8090/ws/alerts` |
| Historial de alertas | `GET /api/alerts` con Basic Auth, auto-refresh cada 15 s |
| Simulador de sensores | Formulario con presets → `POST /api/{sensor}/events` con Basic Auth |
| Notificaciones toast | Al recibir nueva alerta via WebSocket |
| UI basada en rol | SENSOR: solo envío de lecturas; OPERATOR/ADMIN: dashboard + historial |

Estilo oscuro con acentos dorados inspirado en Stark Industries.

---

## 9. Tests E2E — `MicroservicesE2ETest`

**Ubicación:** `gateway-service/src/test/java/.../e2e/MicroservicesE2ETest.java`

Levanta los 5 servicios en la misma JVM en puertos aleatorios (sin broker RabbitMQ ni Eureka). Todos los servicios arrancan con `stark.amqp.enabled=false`, de modo que `HttpAlertPublisher` actúa como único publisher y las alertas llegan al alert-service por HTTP.

**Casos de test:**

| Test | Escenario | Verificación |
|---|---|---|
| `criticalMovementThroughGatewayGeneratesAlert` | `value=1.5` (crítico) | Alerta HIGH con `source` correcto en `/api/alerts` |
| `normalTemperatureThroughGatewayDoesNotCreateCriticalAlert` | `value=24.5` (normal) | No aparece alerta con ese `source` |
| `deniedAccessThroughGatewayGeneratesAlert` | `value=0.0` (crítico) | Alerta HIGH con `source` correcto en `/api/alerts` |

Usa polling (`waitUntil`) hasta 6 s para dar tiempo al procesamiento asíncrono.

---

## 10. Cómo arrancar el sistema

### Con Docker Compose (recomendado)

```bash
docker-compose up --build
```

Orden de arranque automático: `rabbitmq` → `eureka-server` → `alert-service` → sensores → `gateway-service`

### URLs y servicios

| Servicio | URL | Descripción |
|---|---|---|
| Dashboard | http://localhost:8080 | Punto de entrada — login HTML |
| Eureka Dashboard | http://localhost:8761 | Ver microservicios registrados |
| RabbitMQ UI | http://localhost:15672 | Ver colas y mensajes (guest/guest) |
| Alert Service | http://localhost:8090 | Alertas + WebSocket + H2 Console |
| H2 Console | http://localhost:8090/h2-console | `jdbc:h2:mem:alertdb` |
| Movement Sensor | http://localhost:8081 | Sensor de movimiento + sísmico |
| Temperature Sensor | http://localhost:8082 | Sensor de temperatura |
| Access Sensor | http://localhost:8083 | Sensor de acceso + puerta |

### Credenciales

| Usuario | Contraseña | Rol | Acceso |
|---|---|---|---|
| `operator` | `operator-pass` | OPERATOR | Dashboard + historial |
| `admin` | `admin-pass` | ADMIN | Dashboard + historial |
| `sensor-node` | `sensor-pass` | SENSOR | Solo envío de lecturas |

### Ejemplo — probar flujo RabbitMQ completo

```bash
# 1. Enviar lectura crítica de temperatura (≥ 60 °C)
curl -X POST http://localhost:8080/api/temperature/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'sensor-node:sensor-pass' | base64)" \
  -d '{"source":"sensor-sala","value":75.0,"details":"Sala de servidores"}'

# 2. Ver alertas persistidas en H2 (via gateway)
curl http://localhost:8080/api/alerts \
  -H "Authorization: Basic $(echo -n 'operator:operator-pass' | base64)"

# 3. Verificar en RabbitMQ UI → http://localhost:15672
#    Queues → stark.alerts.queue → Get messages
```
