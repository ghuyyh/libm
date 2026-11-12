package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.BookCopy;

public class BookCopyDAO {
    public List<BookCopy> get_copy(String cp_id) {
        String listcopy_q = " SELECT Book_ID, Copy_ID, Shelf_Location, Status, Condition_Notes FROM Book_Copy WHERE Book_ID = ?";
        List<BookCopy> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listcopy_q);
            ) {
            ps.setString(1, cp_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookCopy copy = new BookCopy(
                            rs.getString("Book_ID"),
                            rs.getString("Copy_ID"),
                            rs.getString("Shelf_Location"),
                            rs.getString("Status"),
                            rs.getString("Condition_Notes"));
                    list.add(copy);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}