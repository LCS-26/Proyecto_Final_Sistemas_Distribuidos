package org.toolset.grupo1.access;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SensorAccessApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorAccessApplication.class, args);
    }
}

