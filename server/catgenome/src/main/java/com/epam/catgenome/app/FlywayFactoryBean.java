package com.epam.catgenome.app;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.beans.factory.FactoryBean;
import javax.sql.DataSource;
import java.util.Map;

public class FlywayFactoryBean implements FactoryBean<Flyway> {

    private DataSource dataSource;
    private String[] schemas;
    private String sqlMigrationPrefix = "V"; // default
    private String[] locations;
    private Map<String, String> placeholders;

    // Called by Spring from <property>
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setSchemas(String... schemas) {
        this.schemas = schemas;
    }

    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }

    public void setLocations(String... locations) {
        this.locations = locations;
    }

    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    @Override
    public Flyway getObject() throws Exception {
        var config = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemas)
                .initSql("CREATE SCHEMA IF NOT EXISTS CATGENOME")
                .defaultSchema("CATGENOME")
                .locations(locations)
                .placeholders(placeholders)
                .sqlMigrationPrefix(sqlMigrationPrefix);

        Flyway flyway = config.load();
        flyway.migrate();

        return flyway;
    }

    @Override
    public Class<?> getObjectType() {
        return Flyway.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
