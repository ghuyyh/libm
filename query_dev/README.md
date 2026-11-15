## SQL Queries & Database Reference (`library_queries.sql`)

### 1. Structure of `library_queries.sql`
* **Part 1 — 12 Advanced SQL Queries (Deliverable D5):** Standalone `SELECT` and `VIEW` statements covering multi-table joins, aggregation (`GROUP BY` / `HAVING`), relational division (`NOT EXISTS`), nested queries (`IN` / `EXISTS`), set operations (`UNION`), and views. All queries are calibrated to return non-empty results on the sample dataset.
* **Part 2 — JavaFX UI Queries (Deliverable D6):** Parameterized SQL templates (`?` placeholders for JDBC `PreparedStatement`) mapped to all 8 FXML views (`HomeView`, `BookView`, `MemberView`, `LoanView`, `ReservationView`, `FineView`, `SearchView`, and `ReportView`).

### 2. How to Run in MySQL Workbench
1. Execute the database schema and sample data script first to initialize `library_management_db`, views, triggers, and baseline records.
2. Open `library_queries.sql` in a new query tab and run **Part 1** to verify the 12 analytical queries. *(Note: Part 2 contains `?` parameters intended for Java `PreparedStatement` and should not be executed directly in Workbench).*

### 3. Key Schema & Trigger Notes for Developers
* **Derived Attributes via Views:** Do not query `Total_Copies_Borrowed` on `Loan_Transaction` or `Fine_Amount` on `Loan_Detail` directly; use the **`Loan_Summary`** and **`Loan_Detail_Fine`** views instead.
* **Return & Fine Trigger (BR05):** When updating `Return_Date` in `Loan_Detail`, `Payment_Status` must be updated in the same statement (`'Unpaid'` if returned after `Due_Date`, or `'None'` if returned on/before `Due_Date`).
