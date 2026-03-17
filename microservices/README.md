# Stark Industries Microservices

Arquitectura minima de microservicios para el caso de seguridad concurrente:

- `gateway-service` (puerta de entrada)
- `sensor-movement-service`
- `sensor-temperature-service`
- `sensor-access-service`
- `alert-service`

Cada sensor es un microservicio independiente y notifica eventos criticos a `alert-service`.

## Ejecutar tests

```bash
./mvnw -f microservices/pom.xml test
```

## Ejecutar servicios en local (sin Docker)

Abrir 5 terminales y lanzar:

```bash
./mvnw -f microservices/pom.xml -pl alert-service spring-boot:run
./mvnw -f microservices/pom.xml -pl sensor-movement-service spring-boot:run
./mvnw -f microservices/pom.xml -pl sensor-temperature-service spring-boot:run
./mvnw -f microservices/pom.xml -pl sensor-access-service spring-boot:run
./mvnw -f microservices/pom.xml -pl gateway-service spring-boot:run
```

## Probar flujo rapido via gateway

```bash
curl -X POST http://localhost:8080/api/temperature/events \
  -H 'Content-Type: application/json' \
  -d '{"source":"lab-01","value":72.2,"details":"motor room"}'

curl http://localhost:8080/api/alerts
```

## Docker Compose

```bash
docker compose -f microservices/docker-compose.yml up --build
```

