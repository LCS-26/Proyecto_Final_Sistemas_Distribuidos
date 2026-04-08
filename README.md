# Stark Industries - Sistema de Seguridad Concurrente (Microservicios)

Guia canonica del proyecto `Grupo1`.

## Decisiones cerradas

- WebSocket de alertas: **directo en `alert-service`** (`/ws/alerts`, topic `/topic/alerts`)
- Entrada REST de clientes: **via `gateway-service`** (`http://localhost:8080/api/...`)
- Version Java unificada: **Java 21**
- README canonico: **este archivo `README.md`**

## Arquitectura

- `gateway-service` (puerta de entrada REST + seguridad)
- `sensor-movement-service` (movimiento + sensor sismico de puerta)
- `sensor-temperature-service`
- `sensor-access-service` (acceso + puerta abierta)
- `alert-service` (almacen de alertas en memoria + REST + STOMP/WebSocket)

## Credenciales de prueba (gateway)

- `sensor-node / sensor-pass` -> `ROLE_SENSOR`
- `operator / operator-pass` -> `ROLE_OPERATOR`
- `admin / admin-pass` -> `ROLE_ADMIN`

## Endpoints principales (gateway)

- `POST /api/movement/events`
- `POST /api/seismic/events`
- `POST /api/temperature/events`
- `POST /api/access/events`
- `POST /api/door-open/events`
- `POST /api/access/check`
- `GET /api/alerts`

## WebSocket de alertas

- Endpoint STOMP: `ws://localhost:8090/ws/alerts`
- Topic: `/topic/alerts`

## Ejecutar tests

```bash
./mvnw test
```

## Ejecutar con Docker Compose

```bash
docker compose up --build
```

## Prueba rapida

```bash
curl -u sensor-node:sensor-pass -X POST http://localhost:8080/api/movement/events \
  -H 'Content-Type: application/json' \
  -d '{"source":"cam-01","value":1.4,"details":"main hall"}'

curl -u operator:operator-pass http://localhost:8080/api/alerts
```
