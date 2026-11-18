package com.pdm.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Loan;

public class LoanDAO {

    // Câu SELECT dùng chung: 1 dòng = 1 cuốn sách trong 1 phiếu mượn
    private static final String BASE_SELECT = "SELECT lt.Loan_ID, ld.Detail_ID, lt.Member_ID, m.Full_Name, "
            + "b.Book_ID, b.ISBN, b.Title, ld.Copy_ID, "
            + "lt.Borrow_Date, lt.Due_Date, ld.Return_Date, ld.Payment_Status "
            + "FROM Loan_Transaction lt "
            + "JOIN Loan_Detail ld ON ld.Loan_ID = lt.Loan_ID "
            + "JOIN Member m ON m.Member_ID = lt.Member_ID "
            + "JOIN Book b ON b.Book_ID = ld.Book_ID ";

    private static final String ORDER_BY = " ORDER BY lt.Borrow_Date DESC, lt.Loan_ID, ld.Detail_ID";

    // ------------------------------------------------------------------
    // 1. Danh sách phiếu mượn (nạp lên bảng)
    // ------------------------------------------------------------------
    public List<Loan> getAllLoans() throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(BASE_SELECT + ORDER_BY);
                ResultSet rs = ps.executeQuery()) {
            return readLoans(rs);
        }
    }

    // ------------------------------------------------------------------
    // 2. Tìm kiếm (ô "Enter to search")
    // ------------------------------------------------------------------
    public List<Loan> searchLoans(String keyword) throws SQLException {
        String sql = BASE_SELECT
                + "WHERE lt.Loan_ID LIKE ? OR lt.Member_ID LIKE ? OR m.Full_Name LIKE ? "
                + "OR b.Title LIKE ? OR ld.Copy_ID LIKE ?" + ORDER_BY;
        String pattern = "%" + keyword.trim() + "%";
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return readLoans(rs);
            }
        }
    }

    // ------------------------------------------------------------------
    // 3. Chỉ các sách quá hạn chưa trả (ô "Show overdue today")
    // ------------------------------------------------------------------
    public List<Loan> getOverdueLoans() throws SQLException {
        String sql = BASE_SELECT
                + "WHERE ld.Return_Date IS NULL AND lt.Due_Date < CURRENT_DATE" + ORDER_BY;
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return readLoans(rs);
        }
    }

    // ------------------------------------------------------------------
    // 4. Hỗ trợ tab "New Borrowing": tìm Book_ID theo ISBN,
    //    và tìm một bản copy đang rảnh của sách đó
    // ------------------------------------------------------------------
    public String findBookIdByIsbn(String isbn) throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("SELECT Book_ID FROM Book WHERE ISBN = ?")) {
            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public String findAvailableCopy(String bookId) throws SQLException {
        Connection conn = dbconnect.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Copy_ID FROM Book_Copy WHERE Book_ID = ? AND Status = 'Available' "
                        + "ORDER BY Copy_ID LIMIT 1")) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ------------------------------------------------------------------
    // 5. Tạo phiếu mượn mới (1 phiếu, 1 cuốn). Trả về Loan_ID mới.
    //    Ngày hết hạn = hôm nay + Loan_Period của hạng thành viên.
    //    Các trigger trong DB sẽ tự báo lỗi (SQLException) nếu thành viên
    //    bị khóa/hết hạn hoặc mượn quá giới hạn -> giao diện hiện e.getMessage().
    // ------------------------------------------------------------------
    public String addLoan(String memberId, String staffId, String bookId, String copyId)
            throws SQLException {
        Connection conn = dbconnect.getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            // a) Bản copy phải đang rảnh
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Status FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ? FOR UPDATE")) {
                ps.setString(1, bookId);
                ps.setString(2, copyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Copy " + copyId + " of book " + bookId + " does not exist.");
                    }
                    if (!"Available".equals(rs.getString(1))) {
                        throw new SQLException("This copy is not available (status: " + rs.getString(1) + ").");
                    }
                }
            }

            // b) Tạo Loan_ID mới dạng L18, L19, ...
            String loanId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(MAX(CAST(SUBSTRING(Loan_ID, 2) AS UNSIGNED)), 0) + 1 "
                            + "FROM Loan_Transaction");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                loanId = String.format("L%02d", rs.getInt(1));
            }

            // c) Thêm phiếu mượn
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

            // d) Thêm dòng chi tiết
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID) VALUES (?, 'D01', ?, ?)")) {
                ps.setString(1, loanId);
                ps.setString(2, bookId);
                ps.setString(3, copyId);
                ps.executeUpdate();
            }

            // e) Đánh dấu bản copy đã được mượn
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Book_Copy SET Status = 'Borrowed' WHERE Book_ID = ? AND Copy_ID = ?")) {
                ps.setString(1, bookId);
                ps.setString(2, copyId);
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

    // ------------------------------------------------------------------
    // 6. Trả sách (nút "Returned").
    //    Theo Trigger 3: trả trễ -> 'Unpaid', đúng hạn -> 'None'.
    // ------------------------------------------------------------------
    public void returnBook(String loanId, String detailId) throws SQLException {
        Connection conn = dbconnect.getConnection();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            String bookId;
            String copyId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Book_ID, Copy_ID FROM Loan_Detail "
                            + "WHERE Loan_ID = ? AND Detail_ID = ? AND Return_Date IS NULL")) {
                ps.setString(1, loanId);
                ps.setString(2, detailId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("This book has already been returned or does not exist.");
                    }
                    bookId = rs.getString(1);
                    copyId = rs.getString(2);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Loan_Detail ld "
                            + "JOIN Loan_Transaction lt ON lt.Loan_ID = ld.Loan_ID "
                            + "SET ld.Return_Date = CURRENT_DATE, "
                            + "ld.Payment_Status = IF(CURRENT_DATE > lt.Due_Date, 'Unpaid', 'None') "
                            + "WHERE ld.Loan_ID = ? AND ld.Detail_ID = ?")) {
                ps.setString(1, loanId);
                ps.setString(2, detailId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Book_Copy SET Status = 'Available' WHERE Book_ID = ? AND Copy_ID = ?")) {
                ps.setString(1, bookId);
                ps.setString(2, copyId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    // ------------------------------------------------------------------
    // Hàm phụ: đọc ResultSet thành danh sách Loan
    // ------------------------------------------------------------------
    private List<Loan> readLoans(ResultSet rs) throws SQLException {
        List<Loan> list = new ArrayList<>();
        while (rs.next()) {
            Date borrow = rs.getDate("Borrow_Date");
            Date due = rs.getDate("Due_Date");
            Date ret = rs.getDate("Return_Date");
            list.add(new Loan(
                    rs.getString("Loan_ID"),
                    rs.getString("Detail_ID"),
                    rs.getString("Member_ID"),
                    rs.getString("Full_Name"),
                    rs.getString("Book_ID"),
                    rs.getString("ISBN"),
                    rs.getString("Title"),
                    rs.getString("Copy_ID"),
                    borrow == null ? null : borrow.toLocalDate(),
                    due == null ? null : due.toLocalDate(),
                    ret == null ? null : ret.toLocalDate(),
                    rs.getString("Payment_Status")));
        }
        return list;
    }
}
