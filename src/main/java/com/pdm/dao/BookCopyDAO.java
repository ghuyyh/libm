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
    public List<BookCopy> get_copy(String book_id) {
        String listcopy_q = " SELECT Book_ID, Copy_ID, Shelf_Location, Status, Condition_Notes FROM Book_Copy WHERE Book_ID = ?";
        List<BookCopy> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listcopy_q);) {
            ps.setString(1, book_id);
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
            System.out.println(e.getMessage());
            ;
        }
        return list;
    }

    public List<BookCopy> get_title_available_copy(String book_id) {
        String listcopy_q = """
                SELECT bc.Book_ID, bc.Status, b.Title, bc.Copy_ID, bc.Shelf_Location, bc.Condition_Notes
                FROM Book_Copy bc
                JOIN Book b ON bc.Book_ID = b.Book_ID
                WHERE bc.Book_ID = ?
                AND bc.Status = 'Available'
                        """;
        List<BookCopy> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listcopy_q);) {
            ps.setString(1, book_id);
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
            System.out.println(e.getMessage());
        }
        return list;
    }

    public boolean addCopy(String book_id, String shelf_location, String status, String condition_notes) {
        String copy_id = generateCopyID(book_id);
        String addcopy_q = "INSERT INTO Book_Copy (Book_ID, Copy_ID, Shelf_Location, Status, Condition_Notes) VALUES (?, ?, ?, ?, ?)";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(addcopy_q);) {
            ps.setString(1, book_id);
            ps.setString(2, copy_id);
            ps.setString(3, shelf_location);
            ps.setString(4, status);
            ps.setString(5, condition_notes);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public String generateCopyID(String book_id) {
        String getMaxCopyIDQuery = """
                SELECT Copy_ID
                FROM Book_Copy
                WHERE Book_ID = ?
                ORDER BY CAST(SUBSTRING(Copy_ID, 2) AS UNSIGNED) DESC
                LIMIT 1;
                """;
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(getMaxCopyIDQuery);) {
            ps.setString(1, book_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastCopyID = rs.getString("Copy_ID");
                    int lastNumber = Integer.parseInt(lastCopyID.substring(1));
                    int newNumber = lastNumber + 1;
                    return "C" + newNumber;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return "C01";
    }

    public boolean deleteCopy(String book_id, String copy_id) {
        String deleteCopyQuery = "DELETE FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ?";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(deleteCopyQuery);) {
            ps.setString(1, book_id);
            ps.setString(2, copy_id);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public List<BookCopy> getAllCopies() {
        String query = "SELECT * FROM Book_Copy";
        List<BookCopy> copies = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BookCopy copy = new BookCopy(
                        rs.getString("Book_ID"),
                        rs.getString("Copy_ID"),
                        rs.getString("Shelf_Location"),
                        rs.getString("Status"),
                        rs.getString("Condition_Notes"));
                copies.add(copy);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return copies;
    }
    //AI generated
    public String CopyDeleteChecker(String bookId, String copyId) {
    

     String SQL =
        "SELECT (SELECT COUNT(*) FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ?) AS copy_exists, "
      + "       (SELECT Status FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ?) AS status, "
      + "       (SELECT COUNT(*) FROM Loan_Detail WHERE Book_ID = ? AND Copy_ID = ? "
      + "            AND Return_Date IS NULL) AS on_loan, "
      + "       (SELECT COUNT(*) FROM Loan_Detail WHERE Book_ID = ? AND Copy_ID = ?) AS loan_history, "
      + "       (SELECT COUNT(*) FROM Book_Copy WHERE Book_ID = ?) AS copies_of_title, "
      + "       (SELECT COUNT(*) FROM Reservation WHERE Book_ID = ? "
      + "            AND Status IN ('Pending','Confirmed')) AS open_reservations";
        String b = bookId.trim();
        String c = copyId.trim();
        
        try (
            Connection con = dbconnect.getConnection();
            PreparedStatement ps = con.prepareStatement(SQL)) {
            String[] params = { b, c, b, c, b, c, b, c, b, b };
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt("copy_exists") == 0) {
                    return "This copy no longer exists.";
                }
                if (rs.getInt("on_loan") > 0) {
                    return "This copy is currently on loan.";
                }
                if (rs.getInt("loan_history") > 0) {
                    return "This copy has loan history.";
                }
                String status = rs.getString("status");
                if ("Borrowed".equals(status) || "Reserved".equals(status)) {
                    return "This copy is on Borrowed or wait for Reservation.";
                }
            }
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}