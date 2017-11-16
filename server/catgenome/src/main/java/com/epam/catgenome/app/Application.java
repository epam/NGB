package com.epam.catgenome.app;

import java.io.PrintStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.AuthenticationManagerConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Main entry point for Spring Boot Application
 */
@Import(AppConfiguration.class)
@EnableWebSecurity
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        AuthenticationManagerConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        FallbackWebSecurityAutoConfiguration.class,
        OAuth2AutoConfiguration.class})
public class Application extends SpringBootServletInitializer {

    @Autowired
    private Environment environment;

    @Override protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @EventListener
    public void startupLoggingListener(ApplicationReadyEvent event) {
        print(String.format("NGB Browser started on port: %s (http).",
                                environment.getProperty("local.server.port")), System.out);
    }

    private void print(String message, PrintStream stream) {
        stream.println(message);
    }
}
