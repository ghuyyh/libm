package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.BookTitle;
import com.pdm.model.Publisher;

public class BookTitleDAO {
    public List<BookTitle> get_book() {
        String listbook_q = """
                SELECT b.Book_ID, b.ISBN, b.Title, b.Publication_Year,
                       b.Category_ID, c.Category_Name,
                       b.Publisher_ID, p.Publisher_Name,
                       GROUP_CONCAT(DISTINCT a.Author_Name ORDER BY ba.Author_Order SEPARATOR ', ') AS Authors,
                       COUNT(DISTINCT CASE WHEN bc.Status = 'Available' THEN bc.Copy_ID END) AS Available_Copies
                FROM Book b
                LEFT JOIN Category c ON b.Category_ID = c.Category_ID
                LEFT JOIN Publisher p ON b.Publisher_ID = p.Publisher_ID
                LEFT JOIN Book_Author ba ON b.Book_ID = ba.Book_ID
                LEFT JOIN Author a ON ba.Author_ID = a.Author_ID
                LEFT JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID
                GROUP BY b.Book_ID, b.ISBN, b.Title, b.Publication_Year, b.Category_ID, c.Category_Name, b.Publisher_ID, p.Publisher_Name
                ORDER BY b.Book_ID ASC;
                        """;
        List<BookTitle> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listbook_q);
                ResultSet rs = ps.executeQuery();

        ) {

            while (rs.next()) {
                BookTitle book = new BookTitle(
                        rs.getString("Book_ID"),
                        rs.getString("ISBN"),
                        rs.getString("Title"),
                        rs.getString("Publisher_ID"),
                        rs.getString("Publisher_Name"),
                        rs.getInt("Publication_Year"),
                        rs.getInt("Available_Copies"));
                list.add(book);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public boolean addBook(String isbn, String title, String publisher_ID, int publication_year) {
        String sql = "INSERT INTO Book (Book_ID, Publisher_ID, ISBN, Title, Publication_Year) VALUES (?, ?, ?, ?, ?);";
        String bookID = generateBookID();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, bookID);
            ps.setString(2, publisher_ID);
            ps.setString(3, isbn);
            ps.setString(4, title);
            ps.setInt(5, publication_year);

            int row_updated = ps.executeUpdate();
            return (row_updated > 0);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    private String generateBookID() {
        String sql = """
                SELECT Book_ID
                FROM Book
                ORDER BY CAST(SUBSTRING(Book_ID, 2) AS UNSIGNED) DESC
                LIMIT 1;
                """;
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();) {
            if (rs.next()) {
                String lastBookID = rs.getString("Book_ID");
                int lastNumber = Integer.parseInt(lastBookID.substring(1));
                int newNumber = lastNumber + 1;
                return "B" + newNumber;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return "B01";
    }

    public List<BookTitle> get_top10_borrowed_thisyear(String title) {
        List<BookTitle> list = new ArrayList<>();
        String sql = """
                SELECT b.Book_ID, b.Title, c.Category_Name, COUNT(ld.Detail_ID) AS Total_Borrows
                FROM Book b
                JOIN Category c ON b.Category_ID = c.Category_ID
                JOIN Loan_Detail ld ON b.Book_ID = ld.Book_ID
                JOIN Loan_Transaction lt ON ld.Loan_ID = lt.Loan_ID
                WHERE YEAR(lt.Borrow_Date) = YEAR(CURRENT_DATE)
                GROUP BY b.Book_ID, b.Title, c.Category_Name
                ORDER BY Total_Borrows DESC
                LIMIT 10;
                """;

        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();) {
            while (rs.next()) {
                BookTitle book = new BookTitle(
                        rs.getString("Book_ID"),
                        rs.getString("Title"),
                        rs.getInt("Total_Borrows"));
                list.add(book);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<BookTitle> search_book(String text) {
        List<BookTitle> list = new ArrayList<>();
        String sql = """
                SELECT b.Book_ID, b.ISBN, b.Title, b.Publisher_ID, b.Publication_Year, p.Publisher_Name,
                       GROUP_CONCAT(DISTINCT a.Author_Name ORDER BY ba.Author_Order SEPARATOR ', ') AS Authors,
                       COUNT(DISTINCT CASE WHEN bc.Status = 'Available' THEN bc.Copy_ID END) AS Available_Copies
                FROM Book b
                JOIN Publisher p ON b.Publisher_ID = p.Publisher_ID
                LEFT JOIN Book_Author ba ON b.Book_ID = ba.Book_ID
                LEFT JOIN Author a ON ba.Author_ID = a.Author_ID
                LEFT JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID
                WHERE b.Book_ID LIKE ? OR b.Title LIKE ? OR b.ISBN LIKE ? or p.Publisher_Name LIKE ? 
                GROUP BY b.Book_ID, b.ISBN, b.Title, b.Publisher_ID, b.Publication_Year, p.Publisher_Name
                ORDER BY b.Title ASC;
                    """;

        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, "%" + text + "%");
            ps.setString(2, "%" + text + "%");
            ps.setString(3, "%" + text + "%");
            ps.setString(4, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookTitle book = new BookTitle(
                            rs.getString("Book_ID"),
                            rs.getString("ISBN"),
                            rs.getString("Title"),
                            rs.getString("Publisher_ID"),
                            rs.getString("Publisher_Name"),
                            rs.getInt("Publication_Year"),
                            rs.getInt("Available_Copies"));
                    list.add(book);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public boolean updateBook(String bookID, String isbn, String title, String publisher_ID,
            int publication_year) {

        String sql = "UPDATE Book SET Publisher_ID = ?, ISBN = ?, Title = ?, Publication_Year = ? WHERE Book_ID = ?;";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, publisher_ID);
            ps.setString(2, isbn);
            ps.setString(3, title);
            ps.setInt(4, publication_year);
            ps.setString(5, bookID);

            int row_updated = ps.executeUpdate();
            return (row_updated > 0);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public List<Publisher> get_publishers() {
        String sql = "SELECT * FROM Publisher";
        List<Publisher> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();) {
            while (rs.next()) {
                Publisher publisher = new Publisher(
                        rs.getString("Publisher_ID"),
                        rs.getString("Publisher_Name"),
                        rs.getString("Address"),
                        rs.getString("Contact_Email"));
                list.add(publisher);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public boolean deleteBook(String bookID) {
        String sql = "DELETE FROM Book WHERE Book_ID = ?;";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, bookID);
            int row_deleted = ps.executeUpdate();
            return (row_deleted > 0);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean addPublisher(String publisherName, String address, String contactEmail) {
        String publisherID = generatePublisherID();
        String sql = "INSERT INTO Publisher (Publisher_ID, Publisher_Name, Address, Contact_Email) VALUES (?, ?, ?, ?);";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, publisherID);
            ps.setString(2, publisherName);
            ps.setString(3, address);
            if (contactEmail == null || contactEmail.isEmpty()) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, contactEmail);
            }
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private String generatePublisherID() {
        String sql = """
                SELECT Publisher_ID
                FROM Publisher
                ORDER BY CAST(SUBSTRING(Publisher_ID, 4) AS UNSIGNED) DESC
                LIMIT 1;
                """;
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();) {
            if (rs.next()) {
                String lastPublisherID = rs.getString("Publisher_ID");
                int lastNumber = Integer.parseInt(lastPublisherID.substring(3));
                int newNumber = lastNumber + 1;
                return "PUB" + newNumber;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return "PUB01";
    }

    // AI generated
    public String BookDeleteChecker(String bookId) {
        String SQL = "SELECT (SELECT COUNT(*) FROM Book WHERE Book_ID = ?) AS title_exists, "
                + "       (SELECT COUNT(*) FROM Loan_Detail WHERE Book_ID = ? AND Return_Date IS NULL) AS on_loan, "
                + "       (SELECT COUNT(*) FROM Loan_Detail WHERE Book_ID = ?) AS loan_history, "
                + "       (SELECT COUNT(*) FROM Reservation WHERE Book_ID = ? "
                + "            AND Status IN ('Pending','Confirmed')) AS open_reservations, "
                + "       (SELECT COUNT(*) FROM Book_Copy WHERE Book_ID = ?) AS copies";

        try (Connection conn = dbconnect.getConnection()) {
            String id = bookId.trim();
            try (PreparedStatement ps = conn.prepareStatement(SQL)) {
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, id);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt("title_exists") == 0) {
                        return "This title no longer exists.";
                    }
                    if (rs.getInt("on_loan") > 0) {
                        return "A copy of this title is currently on loan.";
                    }
                    if (rs.getInt("loan_history") > 0) {
                        return "This title has loan history.";
                    }
                    if (rs.getInt("open_reservations") > 0) {
                        return "This title has reservations.";
                    }
                    if (rs.getInt("copies") > 0) {
                        return "This title has copies.";
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

}
