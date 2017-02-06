package com.epam.catgenome.app;

import org.springframework.context.annotation.*;

/**
 * Basic configuration for Spring Boot Application, loads context from XML- configuration file
 */
@Configuration
@ImportResource({
        "classpath*:applicationContext.xml"})
@ComponentScan(basePackages = "com.epam.catgenome",
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION,
                value = org.springframework.stereotype.Controller.class)})
@Import(AppMVCConfiguration.class)
public class AppConfiguration {
}
