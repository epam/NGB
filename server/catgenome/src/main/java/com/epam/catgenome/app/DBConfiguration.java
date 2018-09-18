package com.epam.catgenome.app;

import java.beans.PropertyVetoException;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.mchange.v2.c3p0.ComboPooledDataSource;


public class DBConfiguration {
    @Value("${database.jdbc.url}")
    private String jdbcUrl;

    @Value("${database.username}")
    private String jdbcUsername;

    @Value("${database.password}")
    private String jdbcPassword;

    @Value("${database.driver.class}")
    private String driverClass;

    @Value("${database.max.pool.size}")
    private int maxPoolSize;

    @Value("${database.initial.pool.size}")
    private int initialPoolSize;

    @Bean
    public DataSource dataSource() throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(driverClass);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setInitialPoolSize(initialPoolSize);
        return dataSource;
    }
}
