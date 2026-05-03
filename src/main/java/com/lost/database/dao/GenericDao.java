package com.lost.database.dao;

import com.lost.database.pool.ConnectionPool;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Базовий клас DAO, що надає спільні CRUD-операції через рефлексію.
 *
 * @param <T> тип сутності
 * @param <ID> тип ідентифікатора
 */
public abstract class GenericDao<T, ID> {
    protected final ConnectionPool connectionPool;
    protected final Class<T> entityClass;
    protected final String tableName;

    protected GenericDao(ConnectionPool connectionPool, Class<T> entityClass, String tableName) {
        this.connectionPool = connectionPool;
        this.entityClass = entityClass;
        this.tableName = tableName;
    }

    public Optional<T> findById(ID id) {
        return findByField("id", id).stream().findFirst();
    }

    public List<T> findByField(String fieldName, Object value) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, fieldName);
        return executeQuery(sql, stmt -> stmt.setObject(1, value));
    }

    public List<T> findAll() {
        String sql = String.format("SELECT * FROM %s", tableName);
        return executeQuery(sql, stmt -> {});
    }

    public T save(T entity) {
        ID id = extractId(entity);
        if (id == null) {
            String sql = buildInsertSql(entity);
            List<Object> values = extractEntityValues(entity, false);
            try (Connection connection = connectionPool.getConnection();
                    PreparedStatement statement =
                            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setParameters(statement, values);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        setId(entity, keys.getObject(1));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error executing insert: " + sql, e);
            }
        } else {
            String sql = buildUpdateSql();
            List<Object> values = extractEntityValues(entity, false);
            values.add(id);
            executeUpdate(sql, values);
        }
        return entity;
    }

    public void deleteById(ID id) {
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        executeUpdate(sql, List.of(id));
    }

    protected List<T> executeQuery(String sql, ParameterSetter parameterSetter) {
        try (Connection connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            parameterSetter.setParameters(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> entities = new ArrayList<>();
                while (resultSet.next()) {
                    entities.add(mapResultSetToEntity(resultSet));
                }
                return entities;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query: " + sql, e);
        }
    }

    protected void executeUpdate(String sql, List<Object> parameters) {
        try (Connection connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing update: " + sql, e);
        }
    }

    protected void setParameters(PreparedStatement statement, List<Object> parameters)
            throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    protected String buildInsertSql(T entity) {
        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getName().equals("id")) continue;
            columns.add(camelCaseToSnakeCase(field.getName()));
            placeholders.add("?");
        }
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

    protected String buildUpdateSql() {
        StringJoiner setClause = new StringJoiner(", ");
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getName().equals("id")) continue;
            setClause.add(camelCaseToSnakeCase(field.getName()) + " = ?");
        }
        return String.format("UPDATE %s SET %s WHERE id = ?", tableName, setClause);
    }

    protected List<Object> extractEntityValues(T entity, boolean includeId) {
        List<Object> values = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (!includeId && field.getName().equals("id")) continue;
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null && field.getType() == LocalDateTime.class) {
                    value = Timestamp.valueOf((LocalDateTime) value);
                }
                values.add(value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field: " + field.getName(), e);
            }
        }
        return values;
    }

    protected T mapResultSetToEntity(ResultSet rs) throws SQLException {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);
                String columnName = camelCaseToSnakeCase(field.getName());
                try {
                    Object value = rs.getObject(columnName);
                    if (value != null) {
                        if (field.getType() == LocalDateTime.class && value instanceof Timestamp) {
                            field.set(entity, ((Timestamp) value).toLocalDateTime());
                        } else if (field.getType() == Long.class && value instanceof Number) {
                            field.set(entity, ((Number) value).longValue());
                        } else if (field.getType() == Integer.class && value instanceof Number) {
                            field.set(entity, ((Number) value).intValue());
                        } else if (field.getType() == String.class
                                && value instanceof java.sql.Clob) { // handle CLOBs if any
                            field.set(entity, value.toString());
                        } else {
                            field.set(entity, value);
                        }
                    }
                } catch (SQLException ignored) {
                    // Column might not exist in ResultSet (e.g. JOIN queries), skip safely
                }
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping ResultSet to entity", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected ID extractId(T entity) {
        try {
            Field idField = entityClass.getDeclaredField("id");
            idField.setAccessible(true);
            return (ID) idField.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    protected void setId(T entity, Object idValue) {
        try {
            Field idField = entityClass.getDeclaredField("id");
            idField.setAccessible(true);
            if (idField.getType() == Long.class && idValue instanceof Number) {
                idField.set(entity, ((Number) idValue).longValue());
            } else {
                idField.set(entity, idValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error setting ID", e);
        }
    }

    protected static String camelCaseToSnakeCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    @FunctionalInterface
    protected interface ParameterSetter {
        void setParameters(PreparedStatement statement) throws SQLException;
    }
}
