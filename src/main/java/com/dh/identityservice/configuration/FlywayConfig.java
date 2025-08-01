package com.dh.identityservice.configuration;


import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FlywayConfig {
    @Value("${spring.flyway.locations}")
    private String[] flywayLocations;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Bean
    public Flyway flyway() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("default.name", "'UPLOAD233'");  // Remove extra quotes
        placeholders.put("permission.name", "'hello22234534'");
        placeholders.put("permission.description", "'test134534534ti'");
        Flyway flyway = Flyway.configure()
                .placeholders(placeholders  )
                .placeholderPrefix("@@{")
                .placeholderSuffix("}@@")
                .dataSource(dataSource())
                .locations(flywayLocations)
                .baselineOnMigrate(true)//default baseline is V1
                .baselineVersion("0")
                .load();
        flyway.migrate();//run .sql file, IF VERSION IS NEWER
        //System.out.println("migrating...");
        return flyway;
    }
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(datasourceUrl);
        dataSource.setUsername(datasourceUsername);
        dataSource.setPassword(datasourcePassword);
        return dataSource;
    }
}

