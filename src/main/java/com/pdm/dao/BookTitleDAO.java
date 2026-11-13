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

    public boolean addBook(BookTitle book) {
        //insert into Book (...) 
        return false;
    }

}
