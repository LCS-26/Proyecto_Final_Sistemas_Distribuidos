package org.toolset.grupo1.gateway.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.toolset.grupo1.access.SensorAccessApplication;
import org.toolset.grupo1.alert.AlertServiceApplication;
import org.toolset.grupo1.gateway.GatewayServiceApplication;
import org.toolset.grupo1.movement.SensorMovementApplication;
import org.toolset.grupo1.temperature.SensorTemperatureApplication;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicroservicesE2ETest {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private ConfigurableApplicationContext alertContext;
    private ConfigurableApplicationContext movementContext;
    private ConfigurableApplicationContext temperatureContext;
    private ConfigurableApplicationContext accessContext;
    private ConfigurableApplicationContext gatewayContext;

    private int alertPort;
    private int movementPort;
    private int temperaturePort;
    private int accessPort;
    private int gatewayPort;

    @BeforeAll
    void startServices() {
        alertPort = randomFreePort();
        movementPort = randomFreePort();
        temperaturePort = randomFreePort();
        accessPort = randomFreePort();
        gatewayPort = randomFreePort();

        alertContext = new SpringApplicationBuilder(AlertServiceApplication.class, PermitAllSecurityConfig.class).run(
                "--server.port=" + alertPort,
                "--internal.token=" + INTERNAL_TOKEN,
                "--spring.main.allow-bean-definition-overriding=true"
        );

        movementContext = new SpringApplicationBuilder(SensorMovementApplication.class, PermitAllSecurityConfig.class).run(
                "--server.port=" + movementPort,
                "--alert.service-url=http://localhost:" + alertPort,
                "--internal.token=" + INTERNAL_TOKEN,
                "--spring.main.allow-bean-definition-overriding=true"
        );

        temperatureContext = new SpringApplicationBuilder(SensorTemperatureApplication.class, PermitAllSecurityConfig.class).run(
                "--server.port=" + temperaturePort,
                "--alert.service-url=http://localhost:" + alertPort,
                "--internal.token=" + INTERNAL_TOKEN,
                "--spring.main.allow-bean-definition-overriding=true"
        );

        accessContext = new SpringApplicationBuilder(SensorAccessApplication.class, PermitAllSecurityConfig.class).run(
                "--server.port=" + accessPort,
                "--alert.service-url=http://localhost:" + alertPort,
                "--internal.token=" + INTERNAL_TOKEN,
                "--spring.main.allow-bean-definition-overriding=true"
        );

        gatewayContext = SpringApplication.run(
                GatewayServiceApplication.class,
                "--server.port=" + gatewayPort,
                "--services.movement-url=http://localhost:" + movementPort,
                "--services.temperature-url=http://localhost:" + temperaturePort,
                "--services.access-url=http://localhost:" + accessPort,
                "--services.alert-url=http://localhost:" + alertPort
        );
    }

    @AfterAll
    void stopServices() {
        close(gatewayContext);
        close(accessContext);
        close(temperatureContext);
        close(movementContext);
        close(alertContext);
    }

    @Test
    void criticalMovementThroughGatewayGeneratesAlert() throws Exception {
        String source = "e2e-movement-" + UUID.randomUUID();

        // Endpoint: POST /api/movement/events (gateway)
        String payload = "{\"source\":\"" + source + "\",\"value\":1.5,\"details\":\"hallway\"}";
        HttpResponse<String> response = postGateway("/api/movement/events", payload, "sensor-node", "sensor-pass");

        assertThat(response.statusCode()).isEqualTo(202);

        // Expected: critical alert appears in alert-service list exposed via gateway /api/alerts
        boolean found = waitUntil(Duration.ofSeconds(6), Duration.ofMillis(200), () ->
                hasCriticalAlertForSource(source));

        assertThat(found).isTrue();
    }

    @Test
    void normalTemperatureThroughGatewayDoesNotCreateCriticalAlert() throws Exception {
        String source = "e2e-temperature-" + UUID.randomUUID();

        // Endpoint: POST /api/temperature/events (gateway)
        String payload = "{\"source\":\"" + source + "\",\"value\":24.5,\"details\":\"normal room\"}";
        HttpResponse<String> response = postGateway("/api/temperature/events", payload, "sensor-node", "sensor-pass");

        assertThat(response.statusCode()).isEqualTo(202);

        // Expected: no new critical alert with this source
        boolean appeared = waitUntil(Duration.ofSeconds(3), Duration.ofMillis(200), () ->
                hasCriticalAlertForSource(source));

        assertThat(appeared).isFalse();
    }

    @Test
    void deniedAccessThroughGatewayGeneratesAlert() throws Exception {
        String source = "e2e-access-" + UUID.randomUUID();

        // Endpoint: POST /api/access/events (gateway)
        String payload = "{\"source\":\"" + source + "\",\"value\":0.0,\"details\":\"invalid badge\"}";
        HttpResponse<String> response = postGateway("/api/access/events", payload, "sensor-node", "sensor-pass");

        assertThat(response.statusCode()).isEqualTo(202);

        // Expected: access alert becomes visible in alert-service list through gateway
        boolean found = waitUntil(Duration.ofSeconds(6), Duration.ofMillis(200), () ->
                hasCriticalAlertForSource(source));

        assertThat(found).isTrue();
    }

    private HttpResponse<String> postGateway(String path, String payload, String user, String password)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + gatewayPort + path))
                .timeout(HTTP_TIMEOUT)
                .header("Authorization", basicAuth(user, password))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean hasCriticalAlertForSource(String source) {
        String alertsBody = fetchAlertsBody();
        return alertsBody.contains("\"source\":\"" + source + "\"")
                && alertsBody.contains("\"severity\":\"HIGH\"");
    }

    private String fetchAlertsBody() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + gatewayPort + "/api/alerts"))
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", basicAuth("operator", "operator-pass"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return response.body();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not fetch alerts from gateway", ex);
        }
    }

    private static boolean waitUntil(Duration timeout, Duration pollInterval, Condition condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.test()) {
                return true;
            }
            Thread.sleep(pollInterval.toMillis());
        }
        return false;
    }

    private static String basicAuth(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static void close(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }

    private static int randomFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not allocate random free port", ex);
        }
    }


    @FunctionalInterface
    private interface Condition {
        boolean test();
    }

    @Configuration
    static class PermitAllSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
}

