package com.example.services;

import java.util.ArrayList;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import java.util.stream.Collectors;

@Service
public class DBService {

    private static final Logger logger = LoggerFactory.getLogger(DBService.class);
    private static final Pattern VALID_TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DBService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves a list of all tables in the database
     * @return Comma-separated list of table names
     */
    @Tool(description = "Retrieve available tables in the database")
    public String getTables() {
        try {
            List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            return tables.isEmpty() ? "No tables found" : String.join(", ", tables);
        } catch (DataAccessException e) {
            logger.error("Error retrieving tables", e);
            return "Error retrieving tables: " + e.getMessage();
        }
    }

    /**
     * Validates if a table name contains only alphanumeric characters and underscores
     * @param tableName The table name to validate
     * @return true if the table name is valid, false otherwise
     */
    private boolean isValidTableName(String tableName) {
        return tableName != null && VALID_TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    /**
     * Checks if a table exists in the database
     * @param tableName The table name to check
     * @return true if the table exists, false otherwise
     */
    @Tool(description = "Check if a table exists in the database")
    public boolean tableExists(String tableName) {
        if (!isValidTableName(tableName)) {
            return false;
        }
        
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", 
                Integer.class, 
                tableName
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            logger.error("Error checking if table exists: {}", tableName, e);
            return false;
        }
    }

    /**
     * Retrieves rows from a specified table with pagination
     * @param tableName The name of the table to query
     * @param page The page number (0-based)
     * @param pageSize The number of records per page
     * @return Formatted string representation of the rows
     */
    @Tool(description = "Retrieve rows from a specified table with pagination")
    public String getRowsPaginated(String tableName, int page, int pageSize) {
        if (!isValidTableName(tableName)) {
            return "Invalid table name. Only alphanumeric characters and underscores are allowed.";
        }
        
        if (!tableExists(tableName)) {
            return "Table '" + tableName + "' does not exist.";
        }
        
        if (page < 0) page = 0;
        if (pageSize <= 0) pageSize = 50;
        
        try {
            int offset = page * pageSize;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + tableName + " LIMIT ? OFFSET ?", 
                pageSize, offset
            );
            
            if (rows.isEmpty()) {
                return "No data found in table '" + tableName + "' for page " + page;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Page: ").append(page + 1).append(", Size: ").append(pageSize).append("\n\n");
            
            // Add headers
            Map<String, Object> firstRow = rows.get(0);
            result.append(String.join(" | ", firstRow.keySet())).append("\n");
            result.append("-".repeat(result.length())).append("\n");
            
            // Add data rows
            for (Map<String, Object> row : rows) {
                result.append(row.values().stream()
                    .map(val -> val == null ? "NULL" : val.toString())
                    .collect(Collectors.joining(" | ")))
                    .append("\n");
            }
            return result.toString();
        } catch (DataAccessException e) {
            logger.error("Error retrieving paginated rows from table: {}", tableName, e);
            return "Error retrieving data: " + e.getMessage();
        }
    }

    /**
     * Searches for rows in a table based on WHERE conditions with pagination and support for various operators.
     * @param tableName The name of the table to search
     * @param conditions Map of column names to condition objects with "operator" and "value" fields
     * @param page The page number (0-based)
     * @param pageSize The number of records per page
     * @return Formatted string representation of matching rows
     * Supported operators:
     * - "=" or "EQUALS": Exact match (value: any type)
     * - ">" or "GT": Greater than (value: comparable type)
     * - ">=" or "GTE": Greater than or equal (value: comparable type)
     * - "<" or "LT": Less than (value: comparable type)
     * - "<=" or "LTE": Less than or equal (value: comparable type)
     * - "LIKE": Pattern matching with wildcards (value: string with % or _ wildcards)
     * - "IN": Value in a list (value: List of items)
     * - "BETWEEN": Value between two points (value: List with exactly 2 elements)
     * - "IS NULL": Field is null (value: not used)
     * - "IS NOT NULL": Field is not null (value: not used)
     */
    @Tool(description = "Search for rows in a table using column conditions with various operators")
    public String searchRows(String tableName, Map<String, Map<String, Object>> conditions, Integer page, Integer pageSize) {
        if (!isValidTableName(tableName)) {
            return "Invalid table name. Only alphanumeric characters and underscores are allowed.";
        }

        if (!tableExists(tableName)) {
            return "Table '" + tableName + "' does not exist.";
        }

        if (conditions == null || conditions.isEmpty()) {
            return "No search conditions provided. Use getRowsPaginated for retrieving all rows.";
        }

        // Handle null values for page and pageSize
        if (page == null || page < 0) page = 0;
        if (pageSize == null || pageSize <= 0) pageSize = 50;

        try {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");
            List<Object> params = new ArrayList<>();

            boolean first = true;
            for (Map.Entry<String, Map<String, Object>> entry : conditions.entrySet()) {
                String columnName = entry.getKey();
                Map<String, Object> condition = entry.getValue();

                if (condition == null || !condition.containsKey("operator") || !condition.containsKey("value")) {
                    return "Each condition must have 'operator' and 'value' fields";
                }

                String operator = condition.get("operator").toString().toUpperCase();
                Object value = condition.get("value");

                if (!first) {
                    queryBuilder.append(" AND ");
                }

                // Handle different operators
                switch (operator) {
                    case "=", "EQUALS":
                        queryBuilder.append(columnName).append(" = ?");
                        params.add(value);
                        break;
                    case ">", "GT":
                        queryBuilder.append(columnName).append(" > ?");
                        params.add(value);
                        break;
                    case ">=", "GTE":
                        queryBuilder.append(columnName).append(" >= ?");
                        params.add(value);
                        break;
                    case "<", "LT":
                        queryBuilder.append(columnName).append(" < ?");
                        params.add(value);
                        break;
                    case "<=", "LTE":
                        queryBuilder.append(columnName).append(" <= ?");
                        params.add(value);
                        break;
                    case "LIKE":
                        queryBuilder.append(columnName).append(" LIKE ?");
                        params.add(value);
                        break;
                    case "IN":
                        if (!(value instanceof List)) {
                            return "Value for IN operator must be a list";
                        }
                        List<?> values = (List<?>) value;
                        if (values.isEmpty()) {
                            return "IN list cannot be empty";
                        }
                        queryBuilder.append(columnName).append(" IN (");
                        for (int i = 0; i < values.size(); i++) {
                            queryBuilder.append(i > 0 ? ", ?" : "?");
                            params.add(values.get(i));
                        }
                        queryBuilder.append(")");
                        break;
                    case "BETWEEN":
                        if (!(value instanceof List) || ((List<?>) value).size() != 2) {
                            return "Value for BETWEEN operator must be a list with exactly 2 elements";
                        }
                        List<?> range = (List<?>) value;
                        queryBuilder.append(columnName).append(" BETWEEN ? AND ?");
                        params.add(range.get(0));
                        params.add(range.get(1));
                        break;
                    case "IS NULL":
                        queryBuilder.append(columnName).append(" IS NULL");
                        break;
                    case "IS NOT NULL":
                        queryBuilder.append(columnName).append(" IS NOT NULL");
                        break;
                    default:
                        return "Unsupported operator: " + operator;
                }

                first = false;
            }

            queryBuilder.append(" LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add(page * pageSize);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());

            if (rows.isEmpty()) {
                return "No matching rows found in table '" + tableName + "' for the provided conditions.";
            }

            StringBuilder result = new StringBuilder();
            result.append("Search results - Page: ").append(page + 1).append(", Size: ").append(pageSize).append("\n\n");

            // Add headers
            Map<String, Object> firstRow = rows.get(0);
            result.append(String.join(" | ", firstRow.keySet())).append("\n");
            result.append("-".repeat(result.length())).append("\n");

            // Add data rows
            for (Map<String, Object> row : rows) {
                result.append(row.values().stream()
                    .map(val -> val == null ? "NULL" : val.toString())
                    .collect(Collectors.joining(" | ")))
                  .append("\n");
            }
            return result.toString();
        } catch (DataAccessException e) {
            logger.error("Error searching rows in table: {}", tableName, e);
            return "Error searching data: " + e.getMessage();
        }
    }

    /**
     * Gets the schema information for a specified table
     * @param tableName The name of the table
     * @return Formatted string with column names and types
     */
    @Tool(description = "Get schema information for a specified table")
    public String getTableSchema(String tableName) {
        if (!isValidTableName(tableName)) {
            return "Invalid table name. Only alphanumeric characters and underscores are allowed.";
        }
        
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM " + tableName
            );
            
            if (columns.isEmpty()) {
                return "No schema information found for table '" + tableName + "'";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Schema for table '").append(tableName).append("':\n");
            result.append("Column | Type | Null | Key | Default | Extra\n");
            result.append("-".repeat(60)).append("\n");
            
            for (Map<String, Object> column : columns) {
                result.append(column.values().stream()
                    .map(val -> val == null ? "NULL" : val.toString())
                    .collect(Collectors.joining(" | ")))
                    .append("\n");
            }
            return result.toString();
        } catch (DataAccessException e) {
            logger.error("Error retrieving schema for table: {}", tableName, e);
            return "Error retrieving schema: " + e.getMessage();
        }
    }
}
