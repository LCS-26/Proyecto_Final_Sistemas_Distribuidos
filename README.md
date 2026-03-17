# Stark Industries - Sistema de Seguridad Concurrente (MVP)

MVP en Spring Boot para gestionar eventos de sensores en tiempo real con procesamiento concurrente, control de acceso por roles y alertas por WebSocket.

## Funcionalidades

- Ingesta REST de eventos de sensores (`MOVEMENT`, `TEMPERATURE`, `ACCESS`)
- Procesamiento asincrono con `@Async` y `ThreadPoolTaskExecutor`
- Persistencia con Spring Data JPA sobre H2 en memoria
- Generacion de alertas criticas y publicacion por STOMP/WebSocket
- Seguridad con Spring Security y autenticacion Basic
- Monitorizacion con Actuator (`health`, `info`, `metrics`)

## Credenciales de prueba

- `sensor-node / sensor-pass` -> rol `SENSOR`
- `operator / operator-pass` -> rol `OPERATOR`
- `admin / admin-pass` -> rol `ADMIN`

## Endpoints principales

- `POST /api/sensors/events`
- `GET /api/sensors/events/{type}`
- `POST /api/access/check`
- `GET /api/alerts`
- `GET /actuator/health`
- WebSocket STOMP: `ws://localhost:8080/ws/alerts`, topic `/topic/alerts`

## Ejecutar

```bash
./mvnw spring-boot:run
```

## Probar rapidamente

```bash
curl -u sensor-node:sensor-pass -X POST http://localhost:8080/api/sensors/events \
  -H 'Content-Type: application/json' \
  -d '{"type":"TEMPERATURE","source":"lab-01","value":72.5,"details":"overheat"}'

curl -u operator:operator-pass http://localhost:8080/api/alerts
```

