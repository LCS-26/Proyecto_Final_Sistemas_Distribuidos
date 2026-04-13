# Análisis Completo del Proyecto — Stark Industries Security System

> Asignatura: Programación Concurrente (UAX)
> Práctica: Implementación de un Sistema de Seguridad Concurrente
> Fecha de análisis: Abril 2026

---

## 1. Estructura General del Proyecto

Proyecto **Maven multi-módulo** con Spring Boot 4.0.3 y Java 21.

```
stark-security-microservices/          ← pom.xml raíz (packaging=pom)
├── alert-service/                     ← Puerto 8090
├── gateway-service/                   ← Puerto 8080
├── sensor-movement-service/           ← Puerto 8081
├── sensor-temperature-service/        ← Puerto 8082
├── sensor-access-service/             ← Puerto 8083
├── docker-compose.yml
├── docs/
│   ├── statement-mapping.md
│   ├── performance-benchmark.md
│   └── ANALISIS_PROYECTO.md           ← (este archivo)
└── tools/
    └── benchmark_gateway.py
```

---

## 2. Descripción de Cada Microservicio

### 2.1 `alert-service` (puerto 8090)

**Responsabilidad:** Recibir alertas de los sensores, almacenarlas y retransmitirlas en tiempo real por WebSocket.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `AlertServiceApplication.java` | `@SpringBootApplication` — punto de entrada |
| `AlertController.java` | `POST /internal/alerts` (protected) · `GET /api/alerts` |
| `AlertRequest.java` | Record con severity, message, sensorType, source, value, timestamp |
| `AlertNotificationService.java` | `@Service` — publica en `/topic/alerts` via `SimpMessagingTemplate` |
| `AlertMessage.java` | Record usado para la payload del WebSocket |
| `WebSocketConfig.java` | `@EnableWebSocketMessageBroker` — STOMP en `/ws/alerts`, broker en `/topic` |
| `InternalSecurityConfig.java` | `WebMvcConfigurer` — registra el interceptor en `/internal/**` |
| `InternalTokenInterceptor.java` | `HandlerInterceptor` — valida cabecera `X-Internal-Token` (devuelve 401 si falla) |

**Almacenamiento:** `CopyOnWriteArrayList<AlertRequest>` en memoria (sin persistencia).

**Seguridad interna:** Todos los endpoints `/internal/**` requieren cabecera `X-Internal-Token` con el valor configurado en `${INTERNAL_TOKEN:stark-internal-token}`.

**Monitoring:** Spring Actuator expone `health`, `info`, `metrics`.

**Tests:**
- `AlertServiceApplicationTests` — context load
- `InternalAlertSecurityTest` — valida 401 sin token y 202 con token correcto

---

### 2.2 `gateway-service` (puerto 8080)

**Responsabilidad:** Puerta de entrada única para clientes externos. Autentica, autoriza y enruta peticiones a los microservicios internos.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `GatewayServiceApplication.java` | `@SpringBootApplication` — punto de entrada |
| `SecurityConfig.java` | Spring Security HTTP Basic, usuarios en memoria, roles |
| `GatewayController.java` | `@RestController` — proxy/router hacia los 4 servicios |

**Usuarios y roles (en memoria):**

| Usuario | Contraseña | Rol |
|---|---|---|
| `sensor-node` | `sensor-pass` | `ROLE_SENSOR` |
| `operator` | `operator-pass` | `ROLE_OPERATOR` |
| `admin` | `admin-pass` | `ROLE_ADMIN` |

**Endpoints expuestos:**

| Método | Path | Destino interno | Roles permitidos |
|---|---|---|---|
| POST/GET | `/api/movement/events` | `sensor-movement-service:8081/api/events` | SENSOR, OPERATOR, ADMIN |
| POST/GET | `/api/seismic/events` | `sensor-movement-service:8081/api/seismic/events` | SENSOR, OPERATOR, ADMIN |
| POST/GET | `/api/temperature/events` | `sensor-temperature-service:8082/api/events` | SENSOR, OPERATOR, ADMIN |
| POST/GET | `/api/access/events` | `sensor-access-service:8083/api/events` | SENSOR, OPERATOR, ADMIN |
| POST/GET | `/api/door-open/events` | `sensor-access-service:8083/api/door-open/events` | SENSOR, OPERATOR, ADMIN |
| POST | `/api/access/check` | `sensor-access-service:8083/api/access/check` | OPERATOR, ADMIN |
| GET | `/api/alerts` | `alert-service:8090/api/alerts` | OPERATOR, ADMIN |

**Características:**
- Propaga cabecera `X-Correlation-Id` (genera UUID si no viene en la petición)
- Usa `RestClient` para llamadas HTTP salientes
- URLs de destino configurables via variables de entorno

**Tests:**
- `GatewayServiceApplicationTests` — context load
- `MicroservicesE2ETest` — inicia los 5 servicios en puertos aleatorios y valida flujos end-to-end completos:
  - Movimiento crítico → alerta HIGH generada
  - Temperatura normal → sin alerta crítica
  - Acceso denegado → alerta HIGH generada

---

### 2.3 `sensor-movement-service` (puerto 8081)

**Responsabilidad:** Procesar eventos de sensores de movimiento y sensores sísmicos.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `SensorMovementApplication.java` | `@SpringBootApplication` + `@EnableAsync` |
| `MovementController.java` | `POST/GET /api/events` |
| `MovementDetectionService.java` | Lógica: crítico si `value >= 1.0` |
| `MovementAsyncAlertService.java` | `@Async` — envía alerta al alert-service |
| `SeismicController.java` | `POST/GET /api/seismic/events` |
| `SeismicDoorService.java` | Lógica: crítico si `impactForce >= 7.0` |
| `SeismicAsyncAlertService.java` | `@Async` — envía alerta al alert-service |
| `AlertClient.java` | Cliente HTTP a `alert-service /internal/alerts` |

**Procesamiento concurrente:** Uso de `@EnableAsync` + `@Async` para el envío de alertas sin bloquear el hilo principal.

**Thread-safety:** `CopyOnWriteArrayList` para almacenamiento de eventos.

**Umbrales de criticidad:**
- Movimiento: `value >= 1.0`
- Sísmico: `impactForce >= 7.0`

---

### 2.4 `sensor-temperature-service` (puerto 8082)

**Responsabilidad:** Procesar eventos de sensores de temperatura.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `SensorTemperatureApplication.java` | `@SpringBootApplication` (sin `@EnableAsync`) |
| `TemperatureController.java` | `POST/GET /api/events` — crítico si `value >= 60.0` |
| `AlertClient.java` | Cliente HTTP a `alert-service /internal/alerts` |

**Importante:** Este servicio **NO tiene `@EnableAsync`** — el envío de alertas es **síncrono**, a diferencia del resto de sensores. Es una inconsistencia en el proyecto actual.

**Thread-safety:** `CopyOnWriteArrayList` para almacenamiento de eventos.

**Umbral de criticidad:** `value >= 60.0` (grados)

---

### 2.5 `sensor-access-service` (puerto 8083)

**Responsabilidad:** Gestionar control de acceso de personas y monitorización de puertas.

**Ficheros principales:**

| Fichero | Descripción |
|---|---|
| `SensorAccessApplication.java` | `@SpringBootApplication` + `@EnableAsync` |
| `AccessController.java` | `POST/GET /api/events` — crítico si `value <= 0.0` |
| `AccessPolicyController.java` | `POST /api/access/check` |
| `DoorOpenController.java` | `POST/GET /api/door-open/events` |
| `AccessControlService.java` | Lógica de autorización por badge y área |
| `DoorOpenService.java` | Lógica: crítico si puerta está abierta |
| `DoorOpenAsyncAlertService.java` | `@Async` — envía alerta de puerta |
| `AlertClient.java` | Cliente HTTP a `alert-service /internal/alerts` |

**Lógica de control de acceso (`AccessControlService`):**
1. `!badgeValid` → denegado: "Invalid badge"
2. `area.contains("vault") && !username.startsWith("stark")` → denegado: "Restricted area policy"
3. Si pasa ambas → concedido: "Access granted"

**Thread-safety:** `CopyOnWriteArrayList` para almacenamiento de eventos.

---

## 3. Arquitectura de Comunicación Actual

```
[Cliente externo]
      │  HTTP Basic Auth
      ▼
[gateway-service :8080]  ← Spring Security (SENSOR/OPERATOR/ADMIN)
      │
      │ HTTP REST (RestClient) — síncrono / con X-Internal-Token y X-Correlation-Id
      ├──────────────────────────────────────────────────────────────┐
      ▼                    ▼                    ▼                    ▼
[movement :8081]   [temperature :8082]   [access :8083]    [alert-service :8090]
      │                    │                    │
      │  @Async            │  (síncrono)        │  @Async
      └────────────────────┴────────────────────┘
                           │ HTTP POST /internal/alerts
                           ▼
                   [alert-service :8090]
                           │ STOMP/WebSocket
                           ▼
                   [clientes WebSocket]
                   (topic /topic/alerts)
```

**Punto clave:** La comunicación entre sensores y alert-service es **HTTP REST directo** (punto a punto). No existe ningún broker de mensajería.

---

## 4. Infraestructura Docker

**`docker-compose.yml`** define los 5 servicios con:
- Builds multi-stage (Maven build + JRE final)
- Variables de entorno para URLs y tokens
- Dependencias (`depends_on`) correctamente definidas
- Puertos mapeados al host

```
alert-service    → 8090:8090
movement-service → 8081:8081
temperature-service → 8082:8082
access-service   → 8083:8083
gateway-service  → 8080:8080
```

---

## 5. Conceptos de Spring del Tema 1 — Inventario

| Concepto del Tema | ¿Implementado? | Dónde |
|---|:---:|---|
| `@SpringBootApplication` | ✅ | Todos los microservicios |
| IoC / Contenedor Spring | ✅ | Implícito en todo el proyecto |
| Inyección de dependencias (`@Autowired`) | ✅ | Todos los services y controllers |
| `@Service`, `@RestController`, `@Repository` | ✅ (parcial) | Service y Controller, no Repository |
| `@Component` / `@ComponentScan` | ✅ | Vía `@SpringBootApplication` |
| `@Configuration` + `@Bean` | ✅ | `WebSocketConfig`, `SecurityConfig` |
| `@Async` + `@EnableAsync` | ✅ | movement, access (NO temperature) |
| Ámbitos de beans (Singleton, Prototype…) | ⚠️ | Solo implícito (singleton por defecto), sin `@Scope` explícito |
| `@Primary` / `@Qualifier` | ❌ | No se usan |
| `@PostConstruct` / `@PreDestroy` | ❌ | No implementados |
| Spring Security | ✅ | `gateway-service` (HTTP Basic, roles) |
| WebSocket / STOMP | ✅ | `alert-service` |
| Spring Actuator | ✅ | Todos los servicios |
| Spring Data (JPA/JDBC) | ❌ | No hay capa de persistencia |
| Repositorios / Aggregate Root | ❌ | No existe patrón Repository |
| `@Transactional` | ❌ | Sin base de datos, sin transacciones |
| Connection Pooling (HikariCP) | ❌ | Sin base de datos |
| JDBC / Spring JDBC | ❌ | Sin base de datos |
| Spring Cloud / Eureka | ❌ | URLs hardcodeadas, sin discovery |
| Mensajería / RabbitMQ | ❌ | Comunicación HTTP directa |
| `ExecutorService` explícito | ❌ | Solo `@Async` sin pool configurado |
| Logging estructurado | ✅ | Structured key=value logs |
| Docker / fat jar | ✅ | Dockerfiles con multi-stage build |

---

## 6. LO QUE FALTA — Análisis de Brechas

> Basado en el enunciado del caso práctico y los apuntes del Tema 1.

---

### 6.1 🔴 CRÍTICO — RabbitMQ como orquestador (requisito explícito del usuario)

**Estado actual:** Los sensores envían alertas al `alert-service` mediante llamadas HTTP REST directas (`AlertClient` → `RestClient` → `POST /internal/alerts`). Esto es **acoplamiento fuerte** punto a punto.

**Lo que falta:** Integrar RabbitMQ como broker de mensajería. El flujo debería ser:

```
Sensor detecta evento crítico
        │
        ▼ publica mensaje
   [RabbitMQ Exchange]
        │
        ▼ consume mensaje
   alert-service → WebSocket → cliente
```

**Cambios necesarios:**
1. Añadir `spring-boot-starter-amqp` a todos los módulos sensor y a `alert-service`
2. Añadir servicio `rabbitmq` en `docker-compose.yml`
3. Crear clase `RabbitMQConfig` con `Queue`, `TopicExchange` y `Binding`
4. Reemplazar los `AlertClient` HTTP en cada sensor por un `AlertPublisher` que use `RabbitTemplate`
5. Crear `AlertConsumer` en `alert-service` con `@RabbitListener` que reciba los mensajes y dispare el WebSocket
6. Eliminar (o mantener como fallback) las llamadas HTTP directas `/internal/alerts`

---

### 6.2 🔴 CRÍTICO — Sin capa de persistencia (Spring Data / JPA)

**Estado actual:** Las alertas y eventos se guardan en `CopyOnWriteArrayList` en memoria. Al reiniciar cualquier servicio, se pierden todos los datos.

**Lo que falta:**
- Añadir `spring-boot-starter-data-jpa` y base de datos (H2 para desarrollo, PostgreSQL para producción)
- Crear entidades `@Entity` para `Alert`, `SensorEvent`
- Crear repositorios con `CrudRepository` o `JpaRepository`
- Usar `@Transactional` en los métodos de escritura
- Configurar Connection Pooling (HikariCP ya viene con Spring Boot)

**Conceptos del Tema 1 que se cubrirían:** Spring Data, Repository, Aggregate, @Transactional, Connection Pooling, Spring JDBC/JPA.

---

### 6.3 🟠 IMPORTANTE — `ExecutorService` explícito para `@Async`

**Estado actual:** `@EnableAsync` habilitado pero sin configuración del pool de hilos. Spring usa el `SimpleAsyncTaskExecutor` por defecto, que crea un hilo nuevo por cada tarea (sin límite, sin reutilización).

**Lo que falta:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "sensorTaskExecutor")
    public Executor sensorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sensor-async-");
        executor.initialize();
        return executor;
    }
}
```

El enunciado especifica explícitamente: *"Utilizar @Async y ExecutorService para manejar el procesamiento concurrente"*.

---

### 6.4 🟠 IMPORTANTE — `sensor-temperature-service` sin `@Async`

**Estado actual:** `SensorTemperatureApplication` no tiene `@EnableAsync` y `TemperatureController` llama directamente a `alertClient.sendCriticalAlert()` de forma síncrona, bloqueando el hilo HTTP.

**Lo que falta:**
- Añadir `@EnableAsync` a `SensorTemperatureApplication`
- Crear `TemperatureAsyncAlertService` con `@Async` (igual que en movement y access)
- Llamar al servicio async desde el controller

---

### 6.5 🟠 IMPORTANTE — Ámbitos de beans sin configurar explícitamente

**Estado actual:** Todos los beans son Singleton por defecto, pero nunca se declara `@Scope` de forma explícita ni se aprovechan otros ámbitos.

**Lo que falta:** Al menos demostrar el uso de `@Scope("prototype")` para algún componente que lo justifique (por ejemplo, un builder de eventos o un procesador de petición que deba ser stateful por request).

El Tema 1 dedica una sección completa a los 5 ámbitos: Singleton, Prototype, Request, Session, Global Session.

---

### 6.6 🟡 RECOMENDADO — `@Primary` / `@Qualifier` para resolución de conflictos

**Estado actual:** No hay ninguna interfaz con múltiples implementaciones, por lo que no se puede demostrar la resolución de conflictos entre beans.

**Lo que falta:** Crear al menos una abstracción con dos implementaciones (por ejemplo, `AlertPublisher` con implementación HTTP y con implementación RabbitMQ) y usar `@Primary` para marcar la preferida o `@Qualifier` para inyectar la específica.

---

### 6.7 🟡 RECOMENDADO — `@PostConstruct` / `@PreDestroy`

**Estado actual:** Ningún bean implementa hooks de ciclo de vida.

**Lo que falta:** Añadir inicialización/limpieza controlada. Por ejemplo:
- `@PostConstruct` en `alert-service` para cargar alertas previas desde BD o precargar datos de configuración
- `@PreDestroy` para cerrar conexiones o vaciar colas pendientes

---

### 6.8 🟡 RECOMENDADO — Service Discovery (Eureka)

**Estado actual:** Las URLs de los servicios están hardcodeadas (`${MOVEMENT_SERVICE_URL:http://localhost:8081}`). Si un servicio escala horizontalmente o cambia de puerto, hay que actualizar la configuración manualmente.

**Lo que falta:** Spring Cloud Netflix Eureka o Spring Cloud LoadBalancer. Los apuntes del Tema 1 (sección 3.13) cubren explícitamente Eureka Server y Eureka Client con Zuul.

---

### 6.9 🟡 RECOMENDADO — Notificaciones externas (email/móvil)

**El enunciado del caso práctico dice:** *"Configurar servicios de mensajería para enviar alertas a dispositivos móviles y correos electrónicos."*

**Estado actual:** Solo hay notificación por WebSocket.

**Lo que falta:**
- `spring-boot-starter-mail` para notificaciones por email
- Un servicio `EmailNotificationService` que se dispare cuando llegue una alerta HIGH

---

### 6.10 🟡 RECOMENDADO — Usuarios en base de datos (Spring Security)

**Estado actual:** Usuarios hardcodeados en memoria (`InMemoryUserDetailsManager`).

**Lo que falta:** Con la capa de persistencia añadida, migrar a `JdbcUserDetailsManager` o `UserDetailsService` con repositorio JPA. Esto demuestra la integración Spring Security + Spring Data.

---

## 7. Resumen de Brechas por Prioridad

| Prioridad | Qué falta | Concepto del Tema 1 |
|:---:|---|---|
| 🔴 | **RabbitMQ como orquestador** (requisito del usuario) | Mensajería / Spring AMQP |
| 🔴 | **Capa de persistencia** (Spring Data JPA + BD) | Spring Data, Repository, @Transactional, Connection Pooling |
| 🟠 | **ExecutorService configurado** para @Async | @Async + ExecutorService (enunciado lo pide explícitamente) |
| 🟠 | **@EnableAsync en temperature-service** | Procesamiento concurrente consistente |
| 🟠 | **@Scope explícito** en algún bean | Ámbitos de beans (Tema 1 §3.4) |
| 🟡 | **@Primary / @Qualifier** | Resolución de conflictos (Tema 1 §3.6) |
| 🟡 | **@PostConstruct / @PreDestroy** | Ciclo de vida de beans (Tema 1 §3.3) |
| 🟡 | **Eureka / Service Discovery** | Spring Cloud (Tema 1 §3.13) |
| 🟡 | **Notificaciones email** | Mensajería (enunciado §5) |
| 🟡 | **Usuarios en BD** | Spring Security + Spring Data |

---

## 8. Arquitectura Objetivo (con RabbitMQ)

```
[Cliente externo]
      │  HTTP Basic Auth
      ▼
[gateway-service :8080]  ← Spring Security
      │ HTTP REST
      ├──────────────────────────────────┐
      ▼              ▼                   ▼
[movement :8081]  [temperature :8082]  [access :8083]
      │                 │                 │
      │ AMQP            │ AMQP            │ AMQP
      └─────────────────┴─────────────────┘
                        │
                        ▼
              [RabbitMQ :5672]
              Exchange: stark.alerts
              Queue: alerts.queue
                        │
                        ▼ @RabbitListener
              [alert-service :8090]
                  │           │
                  │ JPA       │ STOMP/WebSocket
                  ▼           ▼
              [H2/PostgreSQL]  [clientes WebSocket]
                              (topic /topic/alerts)
```

---

## 9. Lo que SÍ está bien hecho

- Arquitectura de microservicios modular y escalable
- Spring Boot correctamente configurado en cada módulo
- Spring Security con roles y HTTP Basic funcional
- WebSocket/STOMP para notificaciones en tiempo real
- Spring Actuator en todos los servicios
- Docker Compose con builds reproducibles
- Thread-safety con `CopyOnWriteArrayList`
- `@Async` en movement y access (patrón correcto)
- Correlación de peticiones con `X-Correlation-Id`
- Tests E2E que levantan todos los servicios
- Logging estructurado
- Seguridad interna con token en cabecera HTTP
