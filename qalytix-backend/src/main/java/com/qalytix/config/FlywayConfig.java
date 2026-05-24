package com.qalytix.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(name = "flyway", initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();
    }

    /**
     * Makes the JPA EntityManagerFactory wait for Flyway to finish migrating
     * before validating the schema. Spring Boot 4.x no longer provides this
     * wiring automatically.
     */
    @Configuration
    static class FlywayJpaDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {
        FlywayJpaDependencyConfig() {
            super("flyway");
        }
    }
}
