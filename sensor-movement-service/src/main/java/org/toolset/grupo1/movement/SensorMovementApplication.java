package org.toolset.grupo1.movement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SensorMovementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorMovementApplication.class, args);
    }
}

