package com.epam.catgenome.app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * Basic configuration for Spring Boot Application, loads context from XML- configuration file
 */
@Configuration
@ImportResource({
        "classpath*:applicationContext.xml"})
@ComponentScan(basePackages = "com.epam.catgenome",
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION,
                value = org.springframework.stereotype.Controller.class)})
@Import({AppMVCConfiguration.class, SecurityConfiguration.class})
@PropertySource(value = {
        "classpath:catgenome.properties",
        "file:///${CATGENOME_CONF_DIR:./config}/catgenome.properties",
        "file:./config/catgenome.properties",
        "file:${conf:./config}/catgenome.properties",
        "classpath:version.properties",
        "file:///${CATGENOME_CONF_DIR:./config}/version.properties"},
        ignoreResourceNotFound = true)
public class AppConfiguration {
}
