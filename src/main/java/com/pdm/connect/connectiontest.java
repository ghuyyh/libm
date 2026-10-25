package com.pdm.connect;

import java.sql.Connection;
import java.sql.SQLException;

public class connectiontest {
    public static void main(String[] args) {
        try (Connection conn = dbconnect.getConnection();) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
