package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Author;

public class AuthorDAO {
    public List<Author> get_book_auth(String book_id) {
        String sql = """
                select a.Author_ID, a.Author_Name, a.Biography
                from Book_Author ba join Author a on ba.Author_ID = a.Author_ID
                where ba.Book_ID = ?
                """;
        List<Author> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, book_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Author author = new Author(
                            rs.getString("Author_ID"),
                            rs.getString("Author_Name"),
                            rs.getString("Biography"));
                    list.add(author);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Author> get_auth(String auth_id) {
        String sql = "select Author_ID, Author_Name, Biography from Author where Author_ID = ?";
        List<Author> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, auth_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Author author = new Author(
                            rs.getString("Author_ID"),
                            rs.getString("Author_Name"),
                            rs.getString("Biography"));
                    list.add(author);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}