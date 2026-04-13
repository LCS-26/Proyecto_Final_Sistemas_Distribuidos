package org.toolset.grupo1.movement.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración del pool de hilos para operaciones @Async del movement-service.
 *
 * Conceptos de los apuntes demostrados:
 * - ExecutorService / ThreadPoolTaskExecutor: pool de hilos reutilizables para
 *   procesamiento concurrente. Evita la creación de un hilo nuevo por cada tarea.
 * - @Async usa este executor cuando está configurado como AsyncConfigurer.
 * - @Configuration + @Bean: configuración programática de beans IoC.
 *
 * Parámetros del pool:
 * - corePoolSize: hilos mínimos activos (2)
 * - maxPoolSize: máximo de hilos si la cola está llena (10)
 * - queueCapacity: tareas en espera antes de crear más hilos (100)
 * - threadNamePrefix: prefijo visible en logs y herramientas de monitoreo
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("stark-movement-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        LOGGER.info("event=async_executor_ready source=movement-service threads=2-10 prefix=stark-movement-async-");
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}
