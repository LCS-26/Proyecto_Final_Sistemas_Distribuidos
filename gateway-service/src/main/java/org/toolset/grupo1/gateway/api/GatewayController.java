package org.toolset.grupo1.gateway.api;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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

    @GetMapping("/movement/events")
    public ResponseEntity<String> movementEvents() {
        return forwardGet(movementClient, "/api/events");
    }

    @PostMapping("/seismic/events")
    public ResponseEntity<String> seismic(@RequestBody Map<String, Object> payload) {
        return forwardPost(movementClient, "/api/seismic/events", payload);
    }

    @GetMapping("/seismic/events")
    public ResponseEntity<String> seismicEvents() {
        return forwardGet(movementClient, "/api/seismic/events");
    }

    @PostMapping("/temperature/events")
    public ResponseEntity<String> temperature(@RequestBody Map<String, Object> payload) {
        return forwardPost(temperatureClient, "/api/events", payload);
    }

    @GetMapping("/temperature/events")
    public ResponseEntity<String> temperatureEvents() {
        return forwardGet(temperatureClient, "/api/events");
    }

    @PostMapping("/access/events")
    public ResponseEntity<String> access(@RequestBody Map<String, Object> payload) {
        return forwardPost(accessClient, "/api/events", payload);
    }

    @GetMapping("/access/events")
    public ResponseEntity<String> accessEvents() {
        return forwardGet(accessClient, "/api/events");
    }

    @PostMapping("/door-open/events")
    public ResponseEntity<String> doorOpen(@RequestBody Map<String, Object> payload) {
        return forwardPost(accessClient, "/api/door-open/events", payload);
    }

    @GetMapping("/door-open/events")
    public ResponseEntity<String> doorOpenEvents() {
        return forwardGet(accessClient, "/api/door-open/events");
    }

    @PostMapping("/access/check")
    public ResponseEntity<String> accessCheck(@RequestBody Map<String, Object> payload) {
        return forwardPost(accessClient, "/api/access/check", payload);
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

    private ResponseEntity<String> forwardGet(RestClient client, String path) {
        return client.get()
                .uri(path)
                .retrieve()
                .toEntity(String.class);
    }
}
