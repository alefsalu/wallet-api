package com.portfolio.wallet.infrastructure.observability;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import java.util.Map;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthEndpointController {

    private final HealthEndpoint healthEndpoint;
    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public HealthEndpointController(
            HealthEndpoint healthEndpoint,
            PrometheusMeterRegistry prometheusMeterRegistry) {
        this.healthEndpoint = healthEndpoint;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @GetMapping(value = "/health/live", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping(value = "/health/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> ready() {
        HealthComponent readiness = healthEndpoint.healthForPath("readiness");
        if (readiness != null && Status.UP.equals(readiness.getStatus())) {
            return ResponseEntity.ok(Map.of("status", "UP"));
        }
        HealthComponent overall = healthEndpoint.health();
        if (overall != null && Status.UP.equals(overall.getStatus())) {
            return ResponseEntity.ok(Map.of("status", "UP"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN"));
    }

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> metrics() {
        return ResponseEntity.ok(prometheusMeterRegistry.scrape());
    }
}
