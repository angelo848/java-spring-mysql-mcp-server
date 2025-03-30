package com.example.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbConfig {

  private static final Logger log = LoggerFactory.getLogger(DbConfig.class);
  @Value("${db.host-prefix}")
  private String hostprefix;
  @Value("${db.hostenv}")
  private String hostenv;
  @Value("${db.passwordenv}")
  private String passwordenv;
  @Value("${db.schema}")
  private String schema;
  @Value("${db.user}")
  private String user;
  @Value("${db.ssl-and-timezone}")
  private String sslAndTimezone;

  @Bean
  public DataSource sqlConnection() {
    log.info("Creating SQL Connection with envs: hostprefix={}, hostenv={}, schema={}, user={}, password={}", hostprefix, hostenv, schema, user, passwordenv);
    return DataSourceBuilder.create()
      .type(HikariDataSource.class)
      .url(this.getHost())
      .username(this.user)
      .password(this.passwordenv)
      .build();
  }

  private String getHost() {
    return "jdbc:" + this.hostprefix + this.hostenv + this.schema + sslAndTimezone;
  }
}
