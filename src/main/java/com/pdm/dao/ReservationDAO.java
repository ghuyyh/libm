package com.pdm.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Reservation;

public class ReservationDAO {
    public List<Reservation> getReser() {
        List<Reservation> list = new ArrayList<>();
        String sql = """
                SELECT r.Reservation_ID, r.Member_ID, m.Full_Name AS Member_Name,
                       r.Book_ID, b.Title AS Book_Title, r.Reservation_Date, r.Status,
                       (SELECT COUNT(*) FROM Book_Copy bc WHERE bc.Book_ID = r.Book_ID AND bc.Status = 'Available') AS Available_Copies
                FROM Reservation r
                JOIN Member m ON r.Member_ID = m.Member_ID
                JOIN Book b ON r.Book_ID = b.Book_ID
                ORDER BY r.Reservation_Date ASC;
                    """;
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()

        ) {
            while (rs.next()) {
                String id = rs.getString("Reservation_ID");
                String memid = rs.getString("Member_ID");
                String memname = rs.getString("Member_Name");
                String title = rs.getString("Book_Title");
                Date date = rs.getDate("Reservation_Date");
                String status = rs.getString("Status");
                String bookid = rs.getString("Book_ID");
                int availableCopies = rs.getInt("Available_Copies");
                Reservation reservation = new Reservation(id, memid, memname, bookid, title, date, status,
                        availableCopies);
                list.add(reservation);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public boolean cancelReser(String id) {
        String sql = "UPDATE Reservation SET Status = 'Cancelled' WHERE Reservation_ID = ?";
        try (Connection conn = dbconnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}