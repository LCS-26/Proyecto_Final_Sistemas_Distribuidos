FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl gateway-service -am -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/gateway-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

