package com.example;

import com.example.services.DBService;
import com.zaxxer.hikari.HikariDataSource; // added import
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource; // added import
import org.springframework.util.StringUtils;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(DBService dbService) {
        return MethodToolCallbackProvider.builder().toolObjects(dbService).build();
    }

    @Bean
    public DataSource dataSource() {
        String dbHost = System.getProperty("db_host");
        String dbSchema = System.getProperty("db_schema");
        String username = System.getProperty("db_username");
        String password = System.getProperty("db_password");

        // Validate environment variables
        if (!StringUtils.hasText(dbHost)) {
            throw new IllegalStateException("Database host environment variable 'db_url' is not set");
        }

        if (!StringUtils.hasText(dbSchema)) {
            throw new IllegalStateException("Database host environment variable 'db_schema' is not set");
        }

        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException("Database username environment variable 'db_username' is not set");
        }

        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("Database password environment variable 'db_password' is not set");
        }

        // Format the JDBC URL using the host
        String jdbcUrl = String.format("jdbc:mysql://%s:3306/%s", dbHost, dbSchema);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);

        return ds;
    }
}
