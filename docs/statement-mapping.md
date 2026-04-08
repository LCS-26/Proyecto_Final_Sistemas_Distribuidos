# Statement mapping: enunciado vs. implementación

## 1. Introducción breve

El proyecto implementa un sistema de seguridad distribuido basado en microservicios con Spring Boot. La solución separa la entrada REST, el procesamiento de eventos por tipo de sensor, la gestión de acceso y la emisión de alertas en servicios independientes, conectados entre sí por HTTP y, en el caso de las alertas, por WebSocket.

La arquitectura elegida prioriza modularidad y trazabilidad: `gateway-service` actúa como puerta de entrada, los microservicios de sensores procesan eventos de forma especializada, y `alert-service` centraliza el almacenamiento temporal de alertas y su difusión en tiempo real.

## 2. Mapa “enunciado → implementación”

### Gestión de sensores
**Qué se pedía:** recibir y procesar eventos de distintos sensores.

**Cómo se ha implementado:**
- `sensor-movement-service` procesa eventos de movimiento y eventos sísmicos de puerta.
- `sensor-temperature-service` procesa eventos de temperatura.
- `sensor-access-service` procesa eventos de acceso y eventos de puerta abierta.
- Cada servicio expone endpoints REST propios y mantiene los eventos en memoria.

**Limitaciones / simplificaciones:**
- No hay persistencia en base de datos; el estado se guarda en memoria.
- La lógica de detección es simple y basada en umbrales fijos.

### Modularidad y escalabilidad
**Qué se pedía:** una solución modular que permitiera evolucionar cada parte de forma independiente.

**Cómo se ha implementado:**
- La funcionalidad está dividida por responsabilidades en servicios separados.
- `gateway-service` concentra la entrada REST y reenvía solicitudes a los servicios correspondientes.
- `alert-service` está desacoplado del procesamiento de sensores y actúa como destino común de alertas.

**Limitaciones / simplificaciones:**
- No hay balanceo de carga ni descubrimiento dinámico de servicios; las URLs están configuradas de forma estática.
- La escalabilidad es arquitectónica, pero el despliegue actual sigue siendo sencillo.

### Procesamiento concurrente
**Qué se pedía:** manejar eventos concurrentes y no bloquear el flujo principal.

**Cómo se ha implementado:**
- Los servicios usan estructuras en memoria seguras para concurrencia como `CopyOnWriteArrayList`.
- El envío de alertas críticas se delega en servicios asíncronos con `@Async`.
- El flujo principal registra el evento y, si aplica, dispara la alerta sin cambiar el endpoint de entrada.

**Limitaciones / simplificaciones:**
- No existe una cola externa ni un bus de eventos.
- La concurrencia se resuelve de forma básica con asincronía en memoria.

### Control de acceso
**Qué se pedía:** controlar decisiones de acceso según reglas del sistema.

**Cómo se ha implementado:**
- `sensor-access-service` incluye evaluación de acceso y eventos de puerta abierta.
- En acceso, la lógica aplica reglas sencillas: validez de badge y restricciones por zona.
- En puerta abierta, la apertura se considera un evento crítico y dispara alerta.

**Limitaciones / simplificaciones:**
- No hay integración con un sistema real de identidades o permisos.
- Las reglas son directas y están codificadas de forma explícita.

### Notificaciones en tiempo real
**Qué se pedía:** emitir alertas que pudieran recibirse en tiempo real.

**Cómo se ha implementado:**
- `alert-service` expone `POST /internal/alerts` para recibir alertas internas.
- La alerta se almacena en memoria y se publica por STOMP/WebSocket en `/ws/alerts` con topic `/topic/alerts`.
- Los sensores críticos llaman a `alert-service` mediante un cliente HTTP interno.

**Limitaciones / simplificaciones:**
- El WebSocket está centrado en difusión básica; no hay persistencia ni reintento avanzado.
- La entrega real depende de la disponibilidad del servicio receptor.

### Monitorización y logs
**Qué se pedía:** poder seguir el flujo del sistema y observar qué ocurre en cada paso.

**Cómo se ha implementado:**
- Se han añadido logs en el `gateway-service` al recibir y reenviar peticiones.
- Se han añadido logs en cada sensor al recibir eventos, decidir si son críticos y llamar a `alert-service`.
- `alert-service` registra la recepción interna, el guardado y la emisión WebSocket.
- Los logs usan un formato común con `event`, `source` y, cuando está disponible, `cid` (`X-Correlation-Id`).

**Limitaciones / simplificaciones:**
- No existe un sistema completo de observabilidad (trazas distribuidas, métricas exportadas o correlación automática global).
- El `correlationId` es simple: se reutiliza si llega por cabecera y, si no, se genera uno nuevo.

### Resultados / métricas de rendimiento
**Qué se pedía:** obtener evidencia de rendimiento o comportamiento bajo carga.

**Cómo se ha implementado:**
- Existe un benchmark manual ligero en `docs/performance-benchmark.md` y `tools/benchmark_gateway.py`.
- El benchmark mide el flujo HTTP real en escenarios de movimiento, temperatura y acceso.
- El resultado se usa como evidencia para memoria o presentación, con salida en tabla y ficheros CSV/MD.

**Limitaciones / simplificaciones:**
- No es una suite de carga industrial ni un entorno de benchmarking exhaustivo.
- Las métricas son útiles como evidencia práctica, pero no sustituyen una campaña formal de rendimiento.

## 3. Arquitectura actual resumida

- **`gateway-service`**: punto de entrada REST. Aplica seguridad de acceso, recibe las peticiones del cliente y las reenvía al microservicio correcto.
- **`sensor-movement-service`**: procesa eventos de movimiento y sísmicos de puerta. Decide si el evento es crítico y, si lo es, dispara alerta.
- **`sensor-temperature-service`**: procesa eventos de temperatura. Marca como críticos los valores altos y envía alerta si procede.
- **`sensor-access-service`**: procesa eventos de acceso y de puerta abierta. Evalúa la política de acceso y genera alertas ante condiciones críticas.
- **`alert-service`**: almacena alertas en memoria, expone consulta REST y publica alertas en tiempo real por WebSocket.

**Comunicación entre servicios:**
- El cliente entra por `gateway-service`.
- El gateway reenvía por HTTP a los microservicios de sensores.
- Cuando un sensor detecta criticidad, llama por HTTP interno a `alert-service` usando `X-Internal-Token` y `X-Correlation-Id`.
- `alert-service` notifica por WebSocket a los consumidores conectados.

## 4. Mejoras futuras

- **Persistencia real:** guardar eventos y alertas en una base de datos para no perder estado al reiniciar.
- **Endurecimiento de seguridad interna:** sustituir el token interno mínimo por autenticación entre servicios más robusta.
- **Notificaciones push/email:** añadir canales adicionales además de WebSocket.
- **Observabilidad avanzada:** trazas distribuidas, métricas exportadas y dashboards operativos.
- **Más pruebas E2E/carga:** ampliar cobertura funcional y repetir el benchmark con escenarios más variados y repetibles.

## 5. Cierre

La implementación actual cubre el flujo principal del enunciado de forma clara: entrada por gateway, procesamiento por tipo de sensor, alerta centralizada y difusión en tiempo real. La solución es deliberadamente simple en persistencia y observabilidad, pero suficiente como base técnica para una entrega, una memoria o una presentación.

