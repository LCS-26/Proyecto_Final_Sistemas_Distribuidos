package org.toolset.grupo1.temperature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Aplicación principal del sensor-temperature-service.
 *
 * @EnableAsync: activa el soporte para métodos @Async, permitiendo que Spring
 * ejecute los métodos anotados en hilos separados del ThreadPoolTaskExecutor
 * configurado en AsyncConfig. Sin esta anotación, @Async se ignora y los métodos
 * se ejecutan de forma síncrona en el hilo del llamador.
 */
@SpringBootApplication
@EnableAsync
public class SensorTemperatureApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorTemperatureApplication.class, args);
    }
}
