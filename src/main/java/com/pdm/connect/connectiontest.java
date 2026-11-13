package com.pdm.connect;

import java.sql.Connection;
import java.sql.SQLException;

public class connectiontest {
    public static void main(String[] args) {
        try (Connection conn = dbconnect.getConnection();) {
    System.out.println("Connection successful!");
    System.out.println("URL: " + conn.getMetaData().getURL());
    System.out.println("Database: " + conn.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            System.err.println("Connection failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}