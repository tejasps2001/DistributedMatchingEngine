package com.tejas.distributedmatchingengine;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ProjectConfig {
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ExecutorService settlementHandler() {
        return Executors.newFixedThreadPool(8);
    }
}
