package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Author;
import com.pdm.model.Member;

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
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Author> get_all_auth() {
        String sql = "select Author_ID, Author_Name, Biography from Author";
        List<Author> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
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
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Member> get_mem_who_read_all_book_from_author(String authorID, String authorName) {
        List<Member> list = new ArrayList<>();
        String sql = """
                SELECT m.Member_ID, m.Full_Name, mc.Type_Name
                FROM Member m
                JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
                WHERE EXISTS (
                    SELECT 1 FROM Book_Author ba JOIN Author a ON ba.Author_ID = a.Author_ID
                    WHERE a.Author_ID = ? OR a.Author_Name LIKE ?
                )
                AND NOT EXISTS (
                    SELECT ba.Book_ID FROM Book_Author ba JOIN Author a ON ba.Author_ID = a.Author_ID
                    WHERE (a.Author_ID = ? OR a.Author_Name LIKE ?)
                      AND NOT EXISTS (
                          SELECT 1 FROM Loan_Transaction lt JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
                          WHERE lt.Member_ID = m.Member_ID AND ld.Book_ID = ba.Book_ID
                      )
                );
                """;

        try (Connection conn = dbconnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, authorID);
            ps.setString(2, "%" + authorName + "%");
            ps.setString(3, authorID);
            ps.setString(4, "%" + authorName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Member member = new Member(rs.getString("Member_ID"), rs.getString("Full_Name"));
                    list.add(member);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }
}