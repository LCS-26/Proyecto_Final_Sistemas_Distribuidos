package org.toolset.grupo1.gateway.api;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private final RestClient movementClient;
    private final RestClient temperatureClient;
    private final RestClient accessClient;
    private final RestClient alertClient;

    public GatewayController(
            @Value("${services.movement-url:http://localhost:8081}") String movementUrl,
            @Value("${services.temperature-url:http://localhost:8082}") String temperatureUrl,
            @Value("${services.access-url:http://localhost:8083}") String accessUrl,
            @Value("${services.alert-url:http://localhost:8090}") String alertUrl) {
        this.movementClient = RestClient.builder().baseUrl(movementUrl).build();
        this.temperatureClient = RestClient.builder().baseUrl(temperatureUrl).build();
        this.accessClient = RestClient.builder().baseUrl(accessUrl).build();
        this.alertClient = RestClient.builder().baseUrl(alertUrl).build();
    }

    @PostMapping("/movement/events")
    public ResponseEntity<String> movement(@RequestBody Map<String, Object> payload) {
        return forwardPost(movementClient, "/api/events", payload);
    }

    @PostMapping("/temperature/events")
    public ResponseEntity<String> temperature(@RequestBody Map<String, Object> payload) {
        return forwardPost(temperatureClient, "/api/events", payload);
    }

    @PostMapping("/access/events")
    public ResponseEntity<String> access(@RequestBody Map<String, Object> payload) {
        return forwardPost(accessClient, "/api/events", payload);
    }

    @GetMapping("/alerts")
    public String alerts() {
        return alertClient.get().uri("/api/alerts").retrieve().body(String.class);
    }

    private ResponseEntity<String> forwardPost(RestClient client, String path, Map<String, Object> payload) {
        return client.post()
                .uri(path)
                .body(payload)
                .retrieve()
                .toEntity(String.class);
    }
}

