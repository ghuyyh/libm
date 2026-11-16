package com.pdm.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pdm.connect.dbconnect;
import com.pdm.model.MemCat;
import com.pdm.model.Member;

public class MemberDAO {
    // Member tab uses queries from the existing query files (library_queries.sql / query_dev/library_queries.sql)
    // and operates on the database schema defined in database_dev/database.sql.

    private static final Path[] QUERY_FILE_CANDIDATES = new Path[] {
            Path.of("library_queries.sql"),
            Path.of("query_dev", "library_queries.sql")
    };

    private static final String MEMBER_LIST_BASE = """
            SELECT m.Member_ID, m.Full_Name, m.Member_Type_ID, mc.Type_Name, mc.Borrowing_Limit, mc.Loan_Period,
                   m.Email, m.Identity_Card, m.Membership_Expiry_Date, m.Account_Status,
                   COUNT(ld.Detail_ID) AS col_countloan
            FROM Member m
            JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
            LEFT JOIN Loan_Transaction lt ON m.Member_ID = lt.Member_ID
            LEFT JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID AND ld.Return_Date IS NULL
            """;

    private static final String MEMBER_LIST_GROUP_ORDER = """
            GROUP BY m.Member_ID, m.Full_Name, m.Member_Type_ID, mc.Type_Name, mc.Borrowing_Limit, mc.Loan_Period,
                     m.Email, m.Identity_Card, m.Membership_Expiry_Date, m.Account_Status
            ORDER BY m.Member_ID ASC
            """;

    public List<Member> get_member() {
        // Based on M1 in query file, extended with member fields required by UI and database schema.
        requireQueryFromFile("M1");
        String sql = MEMBER_LIST_BASE + MEMBER_LIST_GROUP_ORDER;
        return queryMembers(sql, null);
    }

    public List<Member> search_members(String text) {
        // Based on S3 in query file, extended for UI fields.
        requireQueryFromFile("S3");
        String sql = MEMBER_LIST_BASE + """
                 WHERE m.Member_ID LIKE ? OR m.Full_Name LIKE ? OR m.Identity_Card LIKE ? OR m.Email LIKE ?
                    OR m.Account_Status LIKE ? OR mc.Type_Name LIKE ?
                """ + MEMBER_LIST_GROUP_ORDER;
        String like = "%" + text + "%";
        return queryMembers(sql, ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setString(6, like);
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Member> queryMembers(String sql, Binder binder) {
        List<Member> list = new ArrayList<>();
        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date expiry = rs.getDate("Membership_Expiry_Date");
                    list.add(new Member(
                            rs.getString("Member_ID"),
                            rs.getString("Full_Name"),
                            rs.getString("Member_Type_ID"),
                            rs.getString("Type_Name"),
                            rs.getInt("Borrowing_Limit"),
                            rs.getInt("Loan_Period"),
                            rs.getString("Email"),
                            rs.getString("Identity_Card"),
                            expiry != null ? expiry.toLocalDate() : null,
                            rs.getString("Account_Status")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<MemberCategoryOption> get_member_categories() {
        // M2 in query file provides categories for member screen.
        requireQueryFromFile("M2");
        String sql = """
                SELECT Member_Type_ID, Type_Name, Borrowing_Limit, Loan_Period
                FROM Member_Category
                ORDER BY Member_Type_ID ASC
                """;
        List<MemberCategoryOption> list = new ArrayList<>();
        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MemberCategoryOption(
                        rs.getString("Member_Type_ID"),
                        rs.getString("Type_Name"),
                        rs.getInt("Borrowing_Limit"),
                        rs.getInt("Loan_Period")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addMember(String memberTypeId, String fullName, String email, String identityCard,
            LocalDate expiryDate, String accountStatus) {
        // M4 in query file defines member insert.
        requireQueryFromFile("M4");
        String sql = """
                INSERT INTO Member (Member_ID, Member_Type_ID, Full_Name, Email, Identity_Card, Membership_Expiry_Date, Account_Status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        String memberId = generateMemberID();
        if (memberId == null) {
            return false;
        }
        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            ps.setString(2, memberTypeId);
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, identityCard);
            ps.setDate(6, Date.valueOf(expiryDate));
            ps.setString(7, accountStatus);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean updateMember(String memberId, String memberTypeId, String fullName, String email, String identityCard,
            LocalDate expiryDate, String accountStatus) {
        // M3 in query file defines member update; extended to all editable fields in database schema.
        requireQueryFromFile("M3");
        String sql = """
                UPDATE Member
                SET Member_Type_ID = ?, Full_Name = ?, Email = ?, Identity_Card = ?, Membership_Expiry_Date = ?, Account_Status = ?
                WHERE Member_ID = ?
                """;
        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberTypeId);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, identityCard);
            ps.setDate(5, Date.valueOf(expiryDate));
            ps.setString(6, accountStatus);
            ps.setString(7, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean deleteMember(String memberId) {
        // M5 in query file.
        String sql = requireQueryFromFile("M5");
        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private String requireQueryFromFile(String key) {
        for (Path path : QUERY_FILE_CANDIDATES) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String sql = extractQuery(content, key);
                if (sql != null && !sql.isBlank()) {
                    return sql.trim();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read query file: " + path, e);
            }
        }
        throw new IllegalStateException("Query '" + key + "' not found in library_queries.sql/query_dev/library_queries.sql");
    }

    private String extractQuery(String content, String key) {
        Pattern marker = Pattern.compile("^\\s*--\\s*" + Pattern.quote(key) + "\\b.*$", Pattern.MULTILINE);
        Matcher matcher = marker.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        int bodyStart = matcher.end();
        Matcher next = Pattern.compile("^\\s*--\\s*[A-Z]{1,4}\\d+[A-Za-z]?\\b.*$", Pattern.MULTILINE).matcher(content);
        int bodyEnd = content.length();
        while (next.find()) {
            if (next.start() > matcher.start()) {
                bodyEnd = next.start();
                break;
            }
        }
        String body = content.substring(bodyStart, bodyEnd);
        StringBuilder out = new StringBuilder();
        boolean inStatement = false;
        for (String rawLine : body.replace("\r\n", "\n").split("\n")) {
            String trimmed = rawLine.trim();
            if (!inStatement) {
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("SELECT") || upper.startsWith("INSERT") || upper.startsWith("UPDATE")
                        || upper.startsWith("DELETE") || upper.startsWith("WITH")) {
                    inStatement = true;
                } else {
                    continue;
                }
            }
            out.append(rawLine).append('\n');
            if (trimmed.endsWith(";")) {
                break;
            }
        }
        String sql = out.toString().trim();
        return sql.isEmpty() ? null : sql;
    }

    private String generateMemberID() {
        String sql = """
                SELECT Member_ID
                FROM Member
                ORDER BY CAST(SUBSTRING(Member_ID, 2) AS UNSIGNED) DESC
                LIMIT 1
                """;
        try (Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String lastId = rs.getString("Member_ID");
                int lastNumber = Integer.parseInt(lastId.substring(1));
                return "M" + (lastNumber + 1);
            }
            return "M1";
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public List<MemCat> unpaid_fine_per_mem_cat() {
        String sql = """
                SELECT mc.Member_Type_ID, mc.Type_Name,
                COUNT(ldf.Detail_ID) AS Unpaid_Count,
                COALESCE(SUM(ldf.Fine_Amount), 0) AS Total_Outstanding_Fines
                FROM Member_Category mc
                LEFT JOIN Member m ON mc.Member_Type_ID = m.Member_Type_ID
                LEFT JOIN Loan_Detail_Fine ldf ON m.Member_ID = ldf.Member_ID AND ldf.Payment_Status = 'Unpaid'
                GROUP BY mc.Member_Type_ID, mc.Type_Name
                ORDER BY Total_Outstanding_Fines DESC;
                """;

        List<MemCat> list = new ArrayList<>();
        try (
                Connection conn = dbconnect.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

        ) {
            while (rs.next()) {
                MemCat memCat = new MemCat(
                        rs.getString("Member_Type_ID"),
                        rs.getString("Type_Name"),
                        rs.getLong("Total_Outstanding_Fines"));
                list.add(memCat);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public static class MemberCategoryOption {
        private final String id;
        private final String name;
        private final int borrowingLimit;
        private final int loanPeriod;

        public MemberCategoryOption(String id, String name, int borrowingLimit, int loanPeriod) {
            this.id = id;
            this.name = name;
            this.borrowingLimit = borrowingLimit;
            this.loanPeriod = loanPeriod;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getBorrowingLimit() {
            return borrowingLimit;
        }

        public int getLoanPeriod() {
            return loanPeriod;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}