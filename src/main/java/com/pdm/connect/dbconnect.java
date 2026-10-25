package com.pdm.connect;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class dbconnect {
    // Database configuration
    // private static final String URL = "jdbc:mysql://localhost:3306/test";
    // private static final String USER = "root";
    // private static final String PASSWORD = "your_password";
    private static Connection connection = null;

    private dbconnect() {
    }

    public static Connection getConnection() throws SQLException {

        Properties setting = new Properties();

        try (InputStream input = new FileInputStream("db.properties")) {
            setting.load(input);
        } catch (IOException ex) {
            throw new SQLException("Error reading db.properties", ex);
        }

        String url = setting.getProperty("db.url");
        String user = setting.getProperty("db.user");
        String password = setting.getProperty("db.password");
        // System.out.println(URL);

        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}