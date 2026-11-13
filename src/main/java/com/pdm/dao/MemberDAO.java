package com.pdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pdm.connect.dbconnect;
import com.pdm.model.Member;

public class MemberDAO {
    public List<Member> get_member() {
        String listmember_q = """
        select m.Member_ID, m.Full_Name, m.Member_Type_ID, mc.Type_Name, mc.Borrowing_Limit, mc.Loan_Period 
        from Member m left join Member_Category mc 
        on m.Member_Type_ID = mc.Member_Type_ID 
        """;
        List<Member> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(listmember_q);
                ResultSet rs = ps.executeQuery();

        ) {

            while (rs.next()) {
                Member member = new Member(
                        rs.getString("Member_ID"),
                        rs.getString("Full_Name"),
                        rs.getString("Member_Type_ID"),
                        rs.getString("Type_Name"),
                        rs.getInt("Borrowing_Limit"),
                        rs.getInt("Loan_Period"));
                list.add(member);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}