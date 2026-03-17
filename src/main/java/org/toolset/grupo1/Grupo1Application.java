package org.toolset.grupo1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Grupo1Application {

    public static void main(String[] args) {
        SpringApplication.run(Grupo1Application.class, args);
    }

}
