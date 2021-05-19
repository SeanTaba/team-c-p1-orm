package com.revature.p1.utils;

import com.revature.p1.repos.DataSource;
import com.revature.p1.utils.annotations.Column;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Objects;

public class EntityManager {

    private final QueryBuilder queryBuilder;

    public EntityManager(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    @SuppressWarnings("unchecked")
    public <T, E> Object get(Class<?> clazz, T id) throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException { //TODO clean this up
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(id);
        E object = (E) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
        try {
            Connection connection = DataSource.getInstance().getConnection();
            PreparedStatement stmt = queryBuilder.createSelectQueryFromClass(clazz, id, connection);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            Field[] fields = clazz.getDeclaredFields();
            while (rs.next()) {
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    Field temp = fields[i - 1];
                    temp.setAccessible(true);
                    if (temp.getAnnotation(Column.class).isTimestamp()) { //Feels hacky. If it is isTimestamp, convert to DateTime.
                        temp.set(object, rs.getTimestamp(i).toLocalDateTime());
                        temp.setAccessible(false);
                        continue;
                    }
                    temp.set(object, rs.getObject(i));
                    temp.setAccessible(false);
                }
            }
            DataSource.getInstance().releaseConnection(connection);
            return object;

        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    public <T> boolean update(T object) {
        try {
            Connection connection = DataSource.getInstance().getConnection();
            connection.setAutoCommit(false);
            PreparedStatement stmt = queryBuilder.createUpdateQueryFromObject(object, connection);
            stmt.executeUpdate();
            connection.commit();
            connection.setAutoCommit(true);
            DataSource.getInstance().releaseConnection(connection);
            return true;
        } catch (SQLException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public <T> boolean save(T object) {
        try {
            Connection connection = DataSource.getInstance().getConnection();
            PreparedStatement stmt = queryBuilder.prepareInsertQueryFromObject(object, connection);
            stmt.executeUpdate();
            DataSource.getInstance().releaseConnection(connection);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public <T> boolean delete (Class<?> clazz, T id) {
        try {
            Connection connection = DataSource.getInstance().getConnection();
            PreparedStatement stmt = queryBuilder.createDeleteQueryFromObject(clazz, id, connection);
            stmt.executeUpdate();
            DataSource.getInstance().releaseConnection(connection);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}