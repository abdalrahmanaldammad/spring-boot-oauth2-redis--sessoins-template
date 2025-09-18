package com.example.app.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable async processing and scheduling
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
