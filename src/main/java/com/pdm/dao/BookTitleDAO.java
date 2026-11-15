package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.BookTitle;

public class BookTitleDAO {
    public List<BookTitle> get_book() {
        String listbook_q = "select Book_ID, ISBN, Title, Publication_Year, Publisher_Name from Book b left join Publisher p on b.Publisher_ID = p.Publisher_ID";
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
                        rs.getString("Publisher_Name"),
                        rs.getInt("Publication_Year"));
                list.add(book);
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
        return null;
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

}
