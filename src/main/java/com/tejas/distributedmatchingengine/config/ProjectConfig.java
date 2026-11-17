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
     */
    @Bean(destroyMethod = "shutdown")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ExecutorService settlementExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
