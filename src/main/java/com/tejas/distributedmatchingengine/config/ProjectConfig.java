package com.tejas.distributedmatchingengine.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ProjectConfig {
    /**
     * Asynchronous settlement thread pool.
     *
     * Configured with daemon threads to prevent unmanaged worker threads from blocking
     * JVM shutdown during container termination or test-suite execution, paired with
     * a graceful shutdown hook managed by the Spring ApplicationContext.
     */
    @Bean(destroyMethod = "shutdown")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ExecutorService settlementExecutor() {
        return Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("settlement-worker-" + thread.threadId());
            return thread;
        });
    }
}
