package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Fine;

public class FineDAO {
    public List<Fine> get_fines() { // chuaw xonmg
        String listfine_q = """
                SELECT ldf.Loan_ID, ldf.Detail_ID, ldf.Member_ID, m.Full_Name AS Member_Name,
                       b.Title AS Book_Title, ldf.Copy_ID, ldf.Due_Date, ldf.Return_Date,
                       ldf.Days_Late, ldf.Fine_Amount, ldf.Payment_Status
                FROM Loan_Detail_Fine ldf
                JOIN Member m ON ldf.Member_ID = m.Member_ID
                JOIN Book b ON ldf.Book_ID = b.Book_ID
                WHERE ldf.Fine_Amount > 0
                ORDER BY ldf.Payment_Status DESC, ldf.Due_Date ASC;
                """;
        List<Fine> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listfine_q);
                ResultSet rs = ps.executeQuery();

        ) {

            while (rs.next()) {
                Fine fine = new Fine(
                        rs.getString("Loan_ID"),
                        rs.getString("Detail_ID"),
                        rs.getString("Member_ID"),
                        rs.getString("Member_Name"),
                        rs.getString("Book_Title"),
                        rs.getString("Copy_ID"),
                        rs.getDate("Due_Date"),
                        rs.getDate("Return_Date"),
                        rs.getInt("Days_Late"),
                        rs.getBigDecimal("Fine_Amount"),
                        rs.getString("Payment_Status")
                );
                list.add(fine);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

}