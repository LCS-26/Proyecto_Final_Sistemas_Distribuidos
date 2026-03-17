FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl sensor-temperature-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/sensor-temperature-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

