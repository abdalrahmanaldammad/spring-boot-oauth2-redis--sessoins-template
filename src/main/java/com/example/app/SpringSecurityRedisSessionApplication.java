package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Main Spring Boot application class for Spring Security Redis Session Demo */
@SpringBootApplication
@EnableTransactionManagement
@EnableConfigurationProperties
public class SpringSecurityRedisSessionApplication {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(SpringSecurityRedisSessionApplication.class);
    app.run(args);
  }
}
