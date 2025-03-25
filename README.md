# Spring Boot MySQL Project

This is a Spring Boot project configured with MySQL and HikariCP connection pool.

## Prerequisites

- Java 17 or later
- Maven 3.6 or later
- MySQL 8.0 or later

## Setup

1. Create a MySQL database named `demo`:
```sql
CREATE DATABASE demo;
```

2. Update the database credentials in `src/main/resources/application.properties` if they differ from the defaults:
- username: root
- password: root

## Running the Application

1. Build the project:
```bash
mvn clean install
```

2. Run the application:
```bash
mvn spring:boot run
```
OR
```
java -jar path/to/jar/java-mysql-mcp-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## Configuration

The application uses the following main configurations:

- Server port: 8080
- HikariCP connection pool size: 10
- Hibernate ddl-auto: update (automatically updates database schema)

You can modify these settings in `src/main/resources/application.properties`

`spring.main.web-application-type: none` is mandatory for connect to server via stdio, otherwise this error will be thrown:
```
2025-03-25T09:21:30.352-03:00  WARN 25352 --- [main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.context.ApplicationContextException: Failed to start bean 'webServerStartStop'
```

## Usage on MCP clients
```
{
  "mcpServers": {
    "spring-ai-mcp-weather": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-jar",
        "/Users/asalles/projects/java-mysql-mcp/target/java-mysql-mcp-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

### Debug
You can use the mcp inspector for testing and debugging with the following command:
```
npx @modelcontextprotocol/inspector java -Dspring.ai.mcp.server.stdio=true -jar /path/to/project/target/java-mysql-mcp-0.0.1-SNAPSHOT.jar
```
The logs output in ./mcp-weather-stdio-server.log will only be shown if running with this tool
