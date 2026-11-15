package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;

public class FineDAO {
    public List<String> get_fine() { // chuaw xonmg
        String listfine_q = """
                SELECT lt.Loan_ID, ld.Detail_ID, lt.Member_ID, m.Full_Name AS Member_Name,
                       b.Book_ID, b.ISBN, b.Title, ld.Copy_ID,
                       lt.Borrow_Date AS Date_Out, lt.Due_Date AS DateDue, ld.Return_Date AS Date_Return,
                       CASE
                           WHEN ld.Return_Date IS NOT NULL THEN 'Returned'
                           WHEN lt.Due_Date < CURRENT_DATE THEN 'Overdue'
                           ELSE 'Borrowing'
                       END AS Status,
                       ld.Payment_Status
                FROM Loan_Transaction lt
                JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
                JOIN Member m ON lt.Member_ID = m.Member_ID
                JOIN Book b ON ld.Book_ID = b.Book_ID
                ORDER BY lt.Borrow_Date DESC, lt.Loan_ID ASC;""";
        List<String> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listfine_q);
                ResultSet rs = ps.executeQuery();

        ) {

            while (rs.next()) {
                String fine = rs.getString("Fine_Amount");
                list.add(fine);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }


}