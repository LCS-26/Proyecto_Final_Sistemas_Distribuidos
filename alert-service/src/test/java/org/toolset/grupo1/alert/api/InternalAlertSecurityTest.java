package org.toolset.grupo1.alert.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "internal.token=test-token"
)
class InternalAlertSecurityTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    @LocalServerPort
    private int port;

    @Test
    void internalAlertsRejectsRequestWithoutToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/internal/alerts")))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sampleAlertBody()))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void internalAlertsAcceptsRequestWithValidToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/internal/alerts")))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", "test-token")
                .POST(HttpRequest.BodyPublishers.ofString(sampleAlertBody()))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String sampleAlertBody() {
        return "{"
                + "\"severity\":\"HIGH\"," 
                + "\"message\":\"integration-test-alert\"," 
                + "\"sensorType\":\"MOVEMENT\"," 
                + "\"source\":\"test-source\"," 
                + "\"value\":1.0," 
                + "\"timestamp\":\"" + Instant.now() + "\""
                + "}";
    }
}

