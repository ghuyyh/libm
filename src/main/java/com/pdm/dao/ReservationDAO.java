package com.pdm.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Reservation;

public class ReservationDAO {

    // RS1 from library_queries.sql (with the available-copy count kept in the model)
    private static final String BASE_SELECT = """
            SELECT r.Reservation_ID, r.Member_ID, m.Full_Name AS Member_Name,
                   r.Book_ID, b.Title AS Book_Title, r.Reservation_Date, r.Status,
                   (SELECT COUNT(*) FROM Book_Copy bc
                    WHERE bc.Book_ID = r.Book_ID AND bc.Status = 'Available') AS Available_Copies
            FROM Reservation r
            JOIN Member m ON r.Member_ID = m.Member_ID
            JOIN Book b ON r.Book_ID = b.Book_ID
            """;

    private static final String ORDER_BY = " ORDER BY r.Reservation_Date ASC, r.Reservation_ID ASC";

    public List<Reservation> getReser() throws SQLException {
        return readReservations(BASE_SELECT + ORDER_BY);
    }

    // Search box on the reservation screen: same columns as RS1, filtered by LIKE.
    public List<Reservation> searchReser(String keyword) throws SQLException {
        String sql = BASE_SELECT
                + "WHERE r.Reservation_ID LIKE ? OR r.Member_ID LIKE ? OR m.Full_Name LIKE ? "
                + "OR b.Title LIKE ? OR r.Status LIKE ?" + ORDER_BY;
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return readReservations(rs);
            }
        }
    }

    // The add form asks for an ISBN, but Reservation stores Book_ID (BR02).
    public String findBookIdByIsbn(String isbn) throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("SELECT Book_ID FROM Book WHERE ISBN = ?")) {
            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // RS2: INSERT INTO Reservation ... Status = 'Pending'.
    // Reservation_ID is generated the same way LoanDAO generates Loan_ID.
    public String addReser(String memberId, String bookId, LocalDate reservationDate) throws SQLException {
        if (!exists("SELECT 1 FROM Member WHERE Member_ID = ?", memberId)) {
            throw new SQLException("Member " + memberId + " does not exist.");
        }
        if (!exists("SELECT 1 FROM Book WHERE Book_ID = ?", bookId)) {
            throw new SQLException("Book " + bookId + " does not exist.");
        }

        String reservationId;
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(Reservation_ID, 4) AS UNSIGNED)), 0) + 1 "
                        + "FROM Reservation");
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            reservationId = String.format("RES%02d", rs.getInt(1));
        }

        LocalDate date = reservationDate == null ? LocalDate.now() : reservationDate;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Reservation (Reservation_ID, Member_ID, Book_ID, Reservation_Date, Status) "
                        + "VALUES (?, ?, ?, ?, 'Pending')")) {
            ps.setString(1, reservationId);
            ps.setString(2, memberId);
            ps.setString(3, bookId);
            ps.setDate(4, Date.valueOf(date));
            ps.executeUpdate();
        }
        return reservationId;
    }

    // Available copies of the selected title, used to fill the fulfill ChoiceBox.
    public List<String> getAvailableCopies(String bookId) throws SQLException {
        List<String> copies = new ArrayList<>();
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Copy_ID FROM Book_Copy WHERE Book_ID = ? AND Status = 'Available' "
                        + "ORDER BY Copy_ID")) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    copies.add(rs.getString("Copy_ID"));
                }
            }
        }
        return copies;
    }

    // RS2 status update, restricted to pending reservations.
    public boolean cancelReser(String id) throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Reservation SET Status = 'Cancelled' "
                        + "WHERE Reservation_ID = ? AND Status = 'Pending'")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Fulfils a reservation in one transaction, using the L4 statements from
     * library_queries.sql: create the loan header/detail, mark the copy
     * Borrowed, then close the reservation as Completed (allowed by the DB
     * CHECK on Reservation.Status).
     *
     * @return the new Loan_ID
     */
    public String fulfillReser(String reservationId, String memberId, String bookId,
            String copyId, String staffId) throws SQLException {
        Connection conn = dbconnect.getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            // Lock the reservation so it cannot be cancelled while we fulfil it.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Member_ID, Book_ID, Status FROM Reservation WHERE Reservation_ID = ? FOR UPDATE")) {
                ps.setString(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Reservation " + reservationId + " does not exist.");
                    }
                    if (!"Pending".equals(rs.getString("Status"))) {
                        throw new SQLException("Only pending reservations can be fulfilled.");
                    }
                    if (!memberId.equals(rs.getString("Member_ID"))
                            || !bookId.equals(rs.getString("Book_ID"))) {
                        throw new SQLException("The selected copy does not match this reservation.");
                    }
                }
            }

            // Lock the copy and make sure it is still available.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Status FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ? FOR UPDATE")) {
                ps.setString(1, bookId);
                ps.setString(2, copyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Copy " + copyId + " does not exist.");
                    }
                    if (!"Available".equals(rs.getString(1))) {
                        throw new SQLException("Copy " + copyId + " is no longer available.");
                    }
                }
            }

            if (!exists("SELECT 1 FROM Staff WHERE Staff_ID = ?", staffId)) {
                throw new SQLException("Staff " + staffId + " does not exist.");
            }

            String loanId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(MAX(CAST(SUBSTRING(Loan_ID, 2) AS UNSIGNED)), 0) + 1 "
                            + "FROM Loan_Transaction");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                loanId = String.format("L%02d", rs.getInt(1));
            }

            // L4a: loan header, due date from the member category's Loan_Period.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Loan_Transaction (Loan_ID, Member_ID, Staff_ID, Borrow_Date, Due_Date) "
                            + "SELECT ?, m.Member_ID, ?, CURRENT_DATE, "
                            + "DATE_ADD(CURRENT_DATE, INTERVAL mc.Loan_Period DAY) "
                            + "FROM Member m "
                            + "JOIN Member_Category mc ON mc.Member_Type_ID = m.Member_Type_ID "
                            + "WHERE m.Member_ID = ?")) {
                ps.setString(1, loanId);
                ps.setString(2, staffId);
                ps.setString(3, memberId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Member " + memberId + " does not exist.");
                }
            }

            // L4b: one loan detail line for the chosen physical copy.
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID, "
                            + "Return_Date, Renewal_Count, Payment_Status) "
                            + "VALUES (?, 'D01', ?, ?, NULL, 0, 'None')")) {
                ps.setString(1, loanId);
                ps.setString(2, bookId);
                ps.setString(3, copyId);
                ps.executeUpdate();
            }

            // L4c + Reservation status update.
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Book_Copy SET Status = 'Borrowed' WHERE Book_ID = ? AND Copy_ID = ?")) {
                ps.setString(1, bookId);
                ps.setString(2, copyId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Reservation SET Status = 'Completed' WHERE Reservation_ID = ?")) {
                ps.setString(1, reservationId);
                ps.executeUpdate();
            }

            conn.commit();
            return loanId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    private boolean exists(String sql, String value) throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Reservation> readReservations(String sql) throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return readReservations(rs);
        }
    }

    private List<Reservation> readReservations(ResultSet rs) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new Reservation(
                    rs.getString("Reservation_ID"),
                    rs.getString("Member_ID"),
                    rs.getString("Member_Name"),
                    rs.getString("Book_ID"),
                    rs.getString("Book_Title"),
                    rs.getDate("Reservation_Date"),
                    rs.getString("Status"),
                    rs.getInt("Available_Copies")));
        }
        return list;
    }
}
