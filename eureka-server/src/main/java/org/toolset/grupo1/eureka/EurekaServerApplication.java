package org.toolset.grupo1.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor Eureka para el descubrimiento de microservicios de Stark Industries.
 *
 * Spring Cloud Netflix Eureka actúa como registro de servicios (Service Registry),
 * permitiendo que los microservicios se registren y descubran entre sí sin necesidad
 * de conocer las URLs exactas de los otros servicios (descubrimiento dinámico).
 *
 * Concepto cubierto en los apuntes:
 * - Spring Cloud: Eureka Server como componente de Service Discovery
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
