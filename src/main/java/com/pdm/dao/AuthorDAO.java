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
                select a.Author_ID, a.Author_Name, a.Biography, ba.Author_Order
                from Book_Author ba join Author a on ba.Author_ID = a.Author_ID
                where ba.Book_ID = ? order by ba.Author_Order
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
                            rs.getString("Biography"),
                            rs.getInt("Author_Order"));
                    list.add(author);
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Author> get_auth(String auth_id) {// no author order
        String sql = "select Author_ID, Author_Name, Biography, from Author where Author_ID = ?";
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

        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
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

    public boolean addAuthor(String authorName, String biography) {
        String authorID = generateAuthorID();
        String sql = "INSERT INTO Author (Author_ID, Author_Name, Biography) VALUES (?, ?, ?);";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, authorID);
            ps.setString(2, authorName);
            ps.setString(3, biography);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }

    }

    public boolean addAuthorToBook(String bookID, String authorID) {
        String sql = """
                        INSERT INTO Book_Author (Book_ID, Author_ID, Author_Order)
                        VALUES (?, ?, ?);
                """;

        int authorOrder = generateAuthorOrder(bookID);
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, bookID);
            ps.setString(2, authorID);
            ps.setInt(3, authorOrder);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean removeAuthorFromBook(String bookID, String authorID, int authorOrder) {
        String deleteSql = """
                DELETE FROM Book_Author
                WHERE Book_ID = ? AND Author_ID = ?;
                """;
        String movingSQL = """
                UPDATE Book_Author
                SET Author_Order = Author_Order - 1
                WHERE Book_ID = ?
                  AND Author_Order > ?
                ORDER BY Author_Order ASC;
                """;

        try (Connection conn = dbconnect.getConnection();
                PreparedStatement deletePs = conn.prepareStatement(deleteSql);
                PreparedStatement compactPs = conn.prepareStatement(movingSQL);) {
            try {
                conn.setAutoCommit(false);

                deletePs.setString(1, bookID);
                deletePs.setString(2, authorID);
                int rowsAffected = deletePs.executeUpdate();

                compactPs.setString(1, bookID);
                compactPs.setInt(2, authorOrder);
                compactPs.executeUpdate();

                conn.commit();
                return rowsAffected > 0;
            } catch (SQLException e) {
                conn.rollback();
                System.out.println(e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public int generateAuthorOrder(String bookID) {
        String sql = "SELECT MAX(Author_Order) AS max_order FROM Book_Author WHERE Book_ID = ?";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, bookID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxOrder = rs.getInt("max_order");
                    return maxOrder + 1;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 1;
    }

    public boolean moveUp(String bookID, String authorID, int authorOrder) {
        String movetemp = """
                UPDATE Book_Author
                SET Author_Order = 9999
                WHERE Book_ID = ? AND Author_ID = ? AND Author_Order = ? AND Author_Order > 1;
                """;
        String moveprevsql = """
                UPDATE Book_Author
                SET Author_Order = ?
                WHERE Book_ID = ? AND Author_Order = ?;
                """;
        String movecurrsql = """
                UPDATE Book_Author
                SET Author_Order = ?
                WHERE Book_ID = ? AND Author_ID = ? AND Author_Order = 9999;
                """;

        try (Connection conn = dbconnect.getConnection();
                PreparedStatement moveToTempPs = conn.prepareStatement(movetemp);
                PreparedStatement movePreviousAuthorPs = conn.prepareStatement(moveprevsql);
                PreparedStatement moveCurrentAuthorPs = conn.prepareStatement(movecurrsql)) {
            try {
                conn.setAutoCommit(false);

                moveToTempPs.setString(1, bookID);
                moveToTempPs.setString(2, authorID);
                moveToTempPs.setInt(3, authorOrder);
                int rowsAffected = moveToTempPs.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    return false;
                }

                movePreviousAuthorPs.setInt(1, authorOrder);
                movePreviousAuthorPs.setString(2, bookID);
                movePreviousAuthorPs.setInt(3, authorOrder - 1);
                movePreviousAuthorPs.executeUpdate();

                moveCurrentAuthorPs.setInt(1, authorOrder - 1);
                moveCurrentAuthorPs.setString(2, bookID);
                moveCurrentAuthorPs.setString(3, authorID);
                moveCurrentAuthorPs.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.out.println(e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean moveDown(String bookID, String authorID, int authorOrder) {
        String movetemp = """
                UPDATE Book_Author ba
                JOIN (SELECT MAX(Author_Order) AS max_ord FROM Book_Author WHERE Book_ID = ?) m
                SET ba.Author_Order = 9999
                WHERE ba.Book_ID = ?
                  AND ba.Author_ID = ?
                  AND ba.Author_Order = ?
                  AND ba.Author_Order < m.max_ord;
                """;
        String movenextsql = """
                UPDATE Book_Author
                SET Author_Order = ?
                WHERE Book_ID = ? AND Author_Order = ?;
                """;
        String movecurrsql = """
                UPDATE Book_Author
                SET Author_Order = ?
                WHERE Book_ID = ? AND Author_ID = ? AND Author_Order = 9999;
                """;

        try (Connection conn = dbconnect.getConnection();
                PreparedStatement moveToTempPs = conn.prepareStatement(movetemp);
                PreparedStatement moveNextAuthorPs = conn.prepareStatement(movenextsql);
                PreparedStatement moveCurrentAuthorPs = conn.prepareStatement(movecurrsql)) {
            try {
                conn.setAutoCommit(false);

                moveToTempPs.setString(1, bookID);
                moveToTempPs.setString(2, bookID);
                moveToTempPs.setString(3, authorID);
                moveToTempPs.setInt(4, authorOrder);
                int rowsAffected = moveToTempPs.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    return false;
                }

                moveNextAuthorPs.setInt(1, authorOrder);
                moveNextAuthorPs.setString(2, bookID);
                moveNextAuthorPs.setInt(3, authorOrder + 1);
                moveNextAuthorPs.executeUpdate();

                moveCurrentAuthorPs.setInt(1, authorOrder + 1);
                moveCurrentAuthorPs.setString(2, bookID);
                moveCurrentAuthorPs.setString(3, authorID);
                moveCurrentAuthorPs.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.out.println(e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public String generateAuthorID() {
        String sql = "SELECT Author_ID FROM Author ORDER BY Author_ID DESC LIMIT 1";
        String newAuthorID = "A001";
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastID = rs.getString("Author_ID");
                    int lastNumber = Integer.parseInt(lastID.substring(4));
                    int newNumber = lastNumber + 1;
                    return "AUTH" + newNumber;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return newAuthorID;
    }
}