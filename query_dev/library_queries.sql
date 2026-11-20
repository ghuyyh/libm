-- ============================================================================
-- PROJECT:     IT079IU - PRINCIPLES OF DATABASE MANAGEMENT (GROUP 4)
-- TOPIC 22:    LIBRARY MANAGEMENT SYSTEM
-- DATABASE:    library_management_db (MySQL 8.4 LTS)
-- AUTHORS:     Nguyen Ngoc Phuong Nam & Nguyen Duc Thinh (Query Developers)
-- ============================================================================
-- [LƯU Ý ĐỒNG BỘ SCHEMA & GIAO DIỆN JAVAFX]
-- 1. Derived Attributes (ERD nét đứt -> Views):
--    - Không gọi `Total_Copies_Borrowed` trên `Loan_Transaction` -> dùng View `Loan_Summary`.
--    - Không gọi `Fine_Amount` hay `Days_Late` trên `Loan_Detail` -> dùng View `Loan_Detail_Fine`.
-- 2. Xử lý `Category_ID` trên bảng `Book`:
--    - Cột `Category_ID` vẫn giữ trong DB với giá trị mặc định `DEFAULT 'CAT01'`.
--    - Phần 1 (D5) vẫn giữ Query 7 thống kê theo `Category`; Phần 2 (UI) đã ẩn `Category`
--      để bên Java không cần hiển thị hoặc truyền `Category_ID` khi thêm/sửa sách.
-- 3. Quy tắc Trigger 3 (Trả sách):
--    - Khi cập nhật `Return_Date` trong `Loan_Detail`, bắt buộc cập nhật kèm `Payment_Status`
--      ('Unpaid' nếu trả sau `Due_Date`, 'None' nếu trả đúng/trước hạn).
-- ============================================================================

USE library_management_db;


-- ############################################################################
-- PART 1: 12 ADVANCED SQL QUERIES (DELIVERABLE D5 - RUN DIRECTLY IN WORKBENCH)
-- Note: Calibrated against sample data to ensure every query returns non-empty results.
-- ############################################################################

-- Query 1 (Topic Q1 - Multi-table Join): Retrieve all physical copies of a specific title ('B01') that are currently available.
SELECT bc.Book_ID, b.Title, bc.Copy_ID, bc.Shelf_Location, bc.Condition_Notes
FROM Book_Copy bc
JOIN Book b ON bc.Book_ID = b.Book_ID
WHERE bc.Book_ID = 'B01'
  AND bc.Status = 'Available';

-- Query 2 (Topic Q2 - Multi-table Join): List all active loans of a specific member ('M01') that are overdue today.
SELECT lt.Loan_ID, ld.Detail_ID, b.Title, ld.Copy_ID, lt.Borrow_Date, lt.Due_Date
FROM Loan_Transaction lt
JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
JOIN Book b ON ld.Book_ID = b.Book_ID
WHERE lt.Member_ID = 'M01'
  AND ld.Return_Date IS NULL
  AND lt.Due_Date < CURRENT_DATE;

-- Query 3 (Topic Q3 - Aggregation & Ranking): Find the ten most borrowed book titles in the current year.
SELECT b.Book_ID, b.Title, COUNT(ld.Detail_ID) AS Borrow_Count
FROM Book b
JOIN Loan_Detail ld ON b.Book_ID = ld.Book_ID
JOIN Loan_Transaction lt ON ld.Loan_ID = lt.Loan_ID
WHERE YEAR(lt.Borrow_Date) = YEAR(CURRENT_DATE)
GROUP BY b.Book_ID, b.Title
ORDER BY Borrow_Count DESC
LIMIT 10;

-- Query 4 (Topic Q4 - Relational Division using NOT EXISTS for "For All"): Find members who have borrowed every title written by a given author ('AUTH01').
SELECT m.Member_ID, m.Full_Name
FROM Member m
WHERE EXISTS (
    SELECT 1 
    FROM Book_Author ba 
    WHERE ba.Author_ID = 'AUTH01'
)
AND NOT EXISTS (
    SELECT ba.Book_ID
    FROM Book_Author ba
    WHERE ba.Author_ID = 'AUTH01'
      AND NOT EXISTS (
          SELECT 1
          FROM Loan_Transaction lt
          JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
          WHERE lt.Member_ID = m.Member_ID
            AND ld.Book_ID = ba.Book_ID
      )
);

-- Query 5 (Topic Q5 - Aggregation with GROUP BY using View Loan_Detail_Fine): Calculate the total unpaid fines outstanding per member category.
SELECT mc.Member_Type_ID, mc.Type_Name, SUM(ldf.Fine_Amount) AS Total_Outstanding_Fines
FROM Member_Category mc
JOIN Member m ON mc.Member_Type_ID = m.Member_Type_ID
JOIN Loan_Detail_Fine ldf ON m.Member_ID = ldf.Member_ID
WHERE ldf.Payment_Status = 'Unpaid'
GROUP BY mc.Member_Type_ID, mc.Type_Name;

-- Query 6 (Outer Join): List all library members and their loan transactions, including members who have never borrowed any books.
SELECT m.Member_ID, m.Full_Name, lt.Loan_ID, lt.Borrow_Date, lt.Due_Date
FROM Member m
LEFT JOIN Loan_Transaction lt ON m.Member_ID = lt.Member_ID;

-- Query 7 (GROUP BY & HAVING): List book categories that have at least 3 titles published before the year 2010.
SELECT c.Category_ID, c.Category_Name, COUNT(b.Book_ID) AS Classic_Book_Count
FROM Category c
JOIN Book b ON c.Category_ID = b.Category_ID
WHERE b.Publication_Year < 2010
GROUP BY c.Category_ID, c.Category_Name
HAVING COUNT(b.Book_ID) >= 3;

-- Query 8 (Nested Query with IN): Find books that currently have pending reservations made by student members.
SELECT b.Book_ID, b.Title, b.ISBN
FROM Book b
WHERE b.Book_ID IN (
    SELECT r.Book_ID
    FROM Reservation r
    JOIN Member m ON r.Member_ID = m.Member_ID
    JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
    WHERE mc.Type_Name LIKE '%Student%'
      AND r.Status = 'Pending'
);

-- Query 9 (Set Operation - UNION): Combine all contact email addresses from both Publishers and Staff members.
SELECT Contact_Email AS Email, 'Publisher' AS Contact_Source
FROM Publisher
WHERE Contact_Email IS NOT NULL
UNION
SELECT Email AS Email, 'Staff' AS Contact_Source
FROM Staff;

-- Query 10 (View): Create a view summarizing the availability status and shelf location of all book copies.
CREATE OR REPLACE VIEW View_Book_Availability AS
SELECT b.Book_ID, b.Title, b.ISBN, bc.Copy_ID, bc.Status, bc.Shelf_Location
FROM Book b
JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID;

-- Query 11 (Nested Query with EXISTS): Find staff members who have handled at least one loan transaction for a staff-category member.
SELECT s.Staff_ID, s.Full_Name, s.Position
FROM Staff s
WHERE EXISTS (
    SELECT 1
    FROM Loan_Transaction lt
    JOIN Member m ON lt.Member_ID = m.Member_ID
    JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
    WHERE lt.Staff_ID = s.Staff_ID
      AND mc.Type_Name LIKE '%Staff%'
);

-- Query 12 (Complex Subquery in HAVING using View Loan_Summary): Find members whose total borrowed copies exceed the average copies borrowed per loan transaction.
SELECT m.Member_ID, m.Full_Name, SUM(ls.Total_Copies_Borrowed) AS Total_Borrowed
FROM Member m
JOIN Loan_Summary ls ON m.Member_ID = ls.Member_ID
GROUP BY m.Member_ID, m.Full_Name
HAVING SUM(ls.Total_Copies_Borrowed) > (
    SELECT AVG(Total_Copies_Borrowed)
    FROM Loan_Summary
);


-- ############################################################################
-- PART 2: JAVAFX UI SQL QUERIES (PREPAREDSTATEMENT - CATEGORY OMITTED FROM UI)
-- Note: Uses `?` placeholders for JDBC PreparedStatement; do not execute directly as a batch.
-- ############################################################################

-- ============================================================================
-- SCREEN 1: HomeView.fxml (Dashboard Quick Statistics - Optional)
-- ============================================================================
SELECT 
    (SELECT COUNT(*) FROM Book) AS Total_Books,
    (SELECT COUNT(*) FROM Member WHERE Account_Status = 'Active') AS Active_Members,
    (SELECT COUNT(*) FROM Loan_Detail WHERE Return_Date IS NULL) AS Active_Loans,
    (SELECT COUNT(*) FROM Reservation WHERE Status = 'Pending') AS Pending_Reservations,
    (SELECT COALESCE(SUM(Fine_Amount), 0) FROM Loan_Detail_Fine WHERE Payment_Status = 'Unpaid') AS Unpaid_Fines;


-- ============================================================================
-- SCREEN 2: BookView.fxml (Book & Physical Copy Management - Category Omitted from UI)
-- ============================================================================
-- B1. Retrieve all book titles (with Publisher, Authors, and Available copy count)
SELECT b.Book_ID, b.ISBN, b.Title, b.Publication_Year,
       b.Publisher_ID, p.Publisher_Name,
       GROUP_CONCAT(DISTINCT a.Author_Name ORDER BY ba.Author_Order SEPARATOR ', ') AS Authors,
       COUNT(DISTINCT CASE WHEN bc.Status = 'Available' THEN bc.Copy_ID END) AS Available_Copies
FROM Book b
JOIN Publisher p ON b.Publisher_ID = p.Publisher_ID
LEFT JOIN Book_Author ba ON b.Book_ID = ba.Book_ID
LEFT JOIN Author a ON ba.Author_ID = a.Author_ID
LEFT JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID
GROUP BY b.Book_ID, b.ISBN, b.Title, b.Publication_Year, b.Publisher_ID, p.Publisher_Name
ORDER BY b.Book_ID ASC;

-- B2. Master-Detail: Retrieve physical copies for a selected book title
SELECT Copy_ID, Shelf_Location, Status, Condition_Notes
FROM Book_Copy
WHERE Book_ID = ?
ORDER BY Copy_ID ASC;

-- B3. Insert / Update / Delete Book Title (MySQL automatically assigns Category_ID = DEFAULT 'CAT01')
INSERT INTO Book (Book_ID, Publisher_ID, ISBN, Title, Publication_Year) VALUES (?, ?, ?, ?, ?);
UPDATE Book SET Publisher_ID = ?, ISBN = ?, Title = ?, Publication_Year = ? WHERE Book_ID = ?;
DELETE FROM Book WHERE Book_ID = ?;

-- B4. Insert / Update / Delete Physical Book Copy (Book_Copy)
INSERT INTO Book_Copy (Book_ID, Copy_ID, Shelf_Location, Status, Condition_Notes) VALUES (?, ?, ?, ?, ?);
UPDATE Book_Copy SET Shelf_Location = ?, Status = ?, Condition_Notes = ? WHERE Book_ID = ? AND Copy_ID = ?;
DELETE FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ?;

-- B5. Book Author Management (Safe Reordering & Deletion on `Book_Author` and `Author`)

-- 1. Retrieve all authors of a selected book ordered by Author_Order
SELECT ba.Book_ID, ba.Author_ID, a.Author_Name, ba.Author_Order
FROM Book_Author ba
JOIN Author a ON ba.Author_ID = a.Author_ID
WHERE ba.Book_ID = ?
ORDER BY ba.Author_Order ASC;

-- 2. Link an existing Author to a Book (automatically appends to the end of Author_Order)
INSERT INTO Book_Author (Book_ID, Author_ID, Author_Order)
SELECT ?, ?, COALESCE(MAX(Author_Order), 0) + 1
FROM Book_Author
WHERE Book_ID = ?;

-- 3. Move Up (Safe 3-step swap using temp slot 9999 + boundary guard Author_Order > 1)
-- Execute all 3 statements in ONE JDBC transaction.
-- IMPORTANT: Check rowsAffected = ps1.executeUpdate(); if 0 (already #1), abort/return immediately!
UPDATE Book_Author
SET Author_Order = 9999
WHERE Book_ID = ? AND Author_ID = ? AND Author_Order = ? AND Author_Order > 1;

UPDATE Book_Author
SET Author_Order = ?          -- Pass: currentOrder
WHERE Book_ID = ? AND Author_Order = ?; -- Pass: Book_ID, currentOrder - 1

UPDATE Book_Author
SET Author_Order = ?          -- Pass: currentOrder - 1
WHERE Book_ID = ? AND Author_ID = ? AND Author_Order = 9999;

-- 4. Move Down (Safe 3-step swap using temp slot 9999 + boundary guard Author_Order < MAX)
-- Execute all 3 statements in ONE JDBC transaction.
-- IMPORTANT: Check rowsAffected = ps1.executeUpdate(); if 0 (already last), abort/return immediately!
UPDATE Book_Author ba
JOIN (SELECT MAX(Author_Order) AS max_ord FROM Book_Author WHERE Book_ID = ?) m
SET ba.Author_Order = 9999
WHERE ba.Book_ID = ?
  AND ba.Author_ID = ?
  AND ba.Author_Order = ?
  AND ba.Author_Order < m.max_ord;

UPDATE Book_Author
SET Author_Order = ?          -- Pass: currentOrder
WHERE Book_ID = ? AND Author_Order = ?; -- Pass: Book_ID, currentOrder + 1

UPDATE Book_Author
SET Author_Order = ?          -- Pass: currentOrder + 1
WHERE Book_ID = ? AND Author_ID = ? AND Author_Order = 9999;

-- 5. Remove an Author from a Book and compact remaining Author_Order values (1, 2, 3...)
-- Execute both statements in ONE JDBC transaction.
DELETE FROM Book_Author
WHERE Book_ID = ? AND Author_ID = ?;

UPDATE Book_Author
SET Author_Order = Author_Order - 1
WHERE Book_ID = ?
  AND Author_Order > ?        -- Pass: deletedAuthorOrder
ORDER BY Author_Order ASC;    -- ORDER BY prevents UNIQUE constraint collision during shift

-- 6. Delete an Author completely from master `Author` table
-- Note: Blocked by FK (ON DELETE RESTRICT) if the author is still assigned to any book in `Book_Author`.
DELETE FROM Author
WHERE Author_ID = ?;

-- ============================================================================
-- SCREEN 3: MemberView.fxml (Controller: MemberManage)
-- ============================================================================
-- M1. Retrieve all members for TableView `mem_table` (with active loan count)
SELECT m.Member_ID AS col_memid,
       m.Full_Name AS col_name,
       mc.Type_Name AS col_cat,
       mc.Borrowing_Limit AS col_lim,
       mc.Loan_Period AS col_period,
       COUNT(ld.Detail_ID) AS col_countloan
FROM Member m
JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
LEFT JOIN Loan_Transaction lt ON m.Member_ID = lt.Member_ID
LEFT JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID AND ld.Return_Date IS NULL
GROUP BY m.Member_ID, m.Full_Name, mc.Type_Name, mc.Borrowing_Limit, mc.Loan_Period
ORDER BY m.Member_ID ASC;

-- M2. Retrieve all membership categories for ChoiceBox `cat_select`
SELECT Member_Type_ID, Type_Name FROM Member_Category ORDER BY Member_Type_ID ASC;

-- M3. Update Button: Update selected member's full name and membership category
UPDATE Member
SET Full_Name = ?,
    Member_Type_ID = (SELECT Member_Type_ID FROM Member_Category WHERE Type_Name = ?)
WHERE Member_ID = ?;

-- M4. Add Member Button: Register a new member (requires Email, Identity_Card, and Membership_Expiry_Date)
INSERT INTO Member (Member_ID, Member_Type_ID, Full_Name, Email, Identity_Card, Membership_Expiry_Date, Account_Status)
VALUES (?, ?, ?, ?, ?, ?, 'Active');

-- M5. Delete Member (blocked by ON DELETE RESTRICT if member has loan history)
DELETE FROM Member WHERE Member_ID = ?;


-- ============================================================================
-- SCREEN 4: LoanView.fxml (Loan & Return Management)
-- ============================================================================
-- L1. Retrieve all loan records for TableView and Details pane (with computed loan status)
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
ORDER BY lt.Borrow_Date DESC, lt.Loan_ID ASC;

-- L2. CheckBox `Show overdue today`: Filter active loans that are past due today
SELECT lt.Loan_ID, ld.Detail_ID, lt.Member_ID, m.Full_Name AS Member_Name,
       b.Book_ID, b.ISBN, b.Title, ld.Copy_ID,
       lt.Borrow_Date AS Date_Out, lt.Due_Date AS DateDue, ld.Return_Date AS Date_Return,
       'Overdue' AS Status, ld.Payment_Status
FROM Loan_Transaction lt
JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
JOIN Member m ON lt.Member_ID = m.Member_ID
JOIN Book b ON ld.Book_ID = b.Book_ID
WHERE ld.Return_Date IS NULL
  AND lt.Due_Date < CURRENT_DATE
ORDER BY lt.Due_Date ASC;

-- L3. `Returned ✓` Button: Record book return and release physical copy (execute within one transaction)
UPDATE Loan_Detail ld
JOIN Loan_Transaction lt ON ld.Loan_ID = lt.Loan_ID
SET ld.Return_Date = CURRENT_DATE,
    ld.Payment_Status = CASE WHEN CURRENT_DATE > lt.Due_Date THEN 'Unpaid' ELSE 'None' END
WHERE ld.Loan_ID = ? AND ld.Detail_ID = ? AND ld.Return_Date IS NULL;

UPDATE Book_Copy SET Status = 'Available' WHERE Book_ID = ? AND Copy_ID = ?;

-- L4. `New loan+` Button: Create loan header, loan detail line, and mark copy as Borrowed (execute within one transaction)
INSERT INTO Loan_Transaction (Loan_ID, Member_ID, Staff_ID, Borrow_Date, Due_Date) VALUES (?, ?, ?, CURRENT_DATE, ?);
INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID, Return_Date, Renewal_Count, Payment_Status) VALUES (?, ?, ?, ?, NULL, 0, 'None');
UPDATE Book_Copy SET Status = 'Borrowed' WHERE Book_ID = ? AND Copy_ID = ?;


-- ============================================================================
-- SCREEN 5: ReservationView.fxml (Controller: ReservationManage)
-- ============================================================================
-- RS1. Retrieve all reservations (with Member Name, Book Title, and Available copy count)
SELECT r.Reservation_ID, r.Member_ID, m.Full_Name AS Member_Name,
       r.Book_ID, b.Title AS Book_Title, r.Reservation_Date, r.Status,
       (SELECT COUNT(*) FROM Book_Copy bc WHERE bc.Book_ID = r.Book_ID AND bc.Status = 'Available') AS Available_Copies
FROM Reservation r
JOIN Member m ON r.Member_ID = m.Member_ID
JOIN Book b ON r.Book_ID = b.Book_ID
ORDER BY r.Reservation_Date ASC;

-- RS2. Create a new reservation (Default status is 'Pending')
INSERT INTO Reservation (Reservation_ID, Member_ID, Book_ID, Reservation_Date, Status)
VALUES (?, ?, ?, CURRENT_DATE, 'Pending');

-- RS3. Confirm Reservation (Execute both statements in ONE JDBC transaction)
-- Step 1: Reserve 1 available physical copy of the book (Check executeUpdate() > 0; if 0, no copy available -> rollback)
UPDATE Book_Copy
SET Status = 'Reserved'
WHERE Book_ID = ?
  AND Status = 'Available'
ORDER BY Copy_ID ASC
LIMIT 1;

-- Step 2: Update reservation status from 'Pending' to 'Confirmed'
UPDATE Reservation
SET Status = 'Confirmed'
WHERE Reservation_ID = ?
  AND Status = 'Pending';

-- RS4. Cancel Reservation (Execute in ONE JDBC transaction)
-- Step 1 (ONLY run this in Java if the reservation's current status is 'Confirmed'):
-- Release 1 'Reserved' copy of this book back to 'Available'
UPDATE Book_Copy
SET Status = 'Available'
WHERE Book_ID = ?
  AND Status = 'Reserved'
ORDER BY Copy_ID ASC
LIMIT 1;

-- Step 2 (Always run): Set reservation status to 'Cancelled'
UPDATE Reservation
SET Status = 'Cancelled'
WHERE Reservation_ID = ?
  AND Status IN ('Pending', 'Confirmed');

-- RS5. Complete Reservation -> Convert to Active Loan (Execute all 5 steps in ONE JDBC transaction)
-- Step 1: Lock and retrieve 1 'Reserved' Copy_ID for this Book_ID (Abort/rollback if ResultSet is empty)
SELECT Copy_ID
FROM Book_Copy
WHERE Book_ID = ?
  AND Status = 'Reserved'
ORDER BY Copy_ID ASC
LIMIT 1
FOR UPDATE;

-- Step 2: Temporarily set copy to 'Available' so Trigger 2 allows inserting into `Loan_Detail`
UPDATE Book_Copy
SET Status = 'Available'
WHERE Book_ID = ?
  AND Copy_ID = ?
  AND Status = 'Reserved';

-- Step 3: Create Loan_Transaction header (automatically calculates Due_Date from Member_Category.Loan_Period)
INSERT INTO Loan_Transaction (Loan_ID, Member_ID, Staff_ID, Borrow_Date, Due_Date)
SELECT ?, ?, ?, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL mc.Loan_Period DAY)
FROM Member m
JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
WHERE m.Member_ID = ?;

-- Step 4: Create Loan_Detail line item (Trigger 2 validates borrowing limit & copy availability)
INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID, Return_Date, Renewal_Count, Payment_Status)
VALUES (?, ?, ?, ?, NULL, 0, 'None');

-- Step 5: Mark physical copy as 'Borrowed' and reservation as 'Completed'
UPDATE Book_Copy
SET Status = 'Borrowed'
WHERE Book_ID = ?
  AND Copy_ID = ?;

UPDATE Reservation
SET Status = 'Completed'
WHERE Reservation_ID = ?
  AND Status = 'Confirmed';


-- ============================================================================
-- SCREEN 6: FineView.fxml (Fine Management - powered by View Loan_Detail_Fine)
-- ============================================================================
-- F1. Retrieve all overdue fine records (with days late and calculated fine amount)
SELECT ldf.Loan_ID, ldf.Detail_ID, ldf.Member_ID, m.Full_Name AS Member_Name,
       b.Title AS Book_Title, ldf.Copy_ID, ldf.Due_Date, ldf.Return_Date,
       ldf.Days_Late, ldf.Fine_Amount, ldf.Payment_Status
FROM Loan_Detail_Fine ldf
JOIN Member m ON ldf.Member_ID = m.Member_ID
JOIN Book b ON ldf.Book_ID = b.Book_ID
WHERE ldf.Fine_Amount > 0
ORDER BY ldf.Payment_Status DESC, ldf.Due_Date ASC;

-- F2. Update fine payment status ('Paid' or 'Waived')
UPDATE Loan_Detail
SET Payment_Status = ?
WHERE Loan_ID = ? AND Detail_ID = ?;


-- ============================================================================
-- SCREEN 7: SearchView.fxml (Global Search - Category Filter Omitted)
-- ============================================================================
-- S1. Search books by Book ID, Title, ISBN, or Author Name
SELECT b.Book_ID, b.ISBN, b.Title, b.Publication_Year, p.Publisher_Name,
       GROUP_CONCAT(DISTINCT a.Author_Name ORDER BY ba.Author_Order SEPARATOR ', ') AS Authors,
       COUNT(DISTINCT CASE WHEN bc.Status = 'Available' THEN bc.Copy_ID END) AS Available_Copies
FROM Book b
JOIN Publisher p ON b.Publisher_ID = p.Publisher_ID
LEFT JOIN Book_Author ba ON b.Book_ID = ba.Book_ID
LEFT JOIN Author a ON ba.Author_ID = a.Author_ID
LEFT JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID
WHERE b.Book_ID LIKE ? OR b.Title LIKE ? OR b.ISBN LIKE ? OR a.Author_Name LIKE ?
GROUP BY b.Book_ID, b.ISBN, b.Title, b.Publication_Year, p.Publisher_Name
ORDER BY b.Title ASC;

-- S2. Topic Q1: Find all currently available physical copies of a specific book title
SELECT bc.Book_ID, b.Title, b.ISBN, bc.Copy_ID, bc.Shelf_Location, bc.Status, bc.Condition_Notes
FROM Book_Copy bc
JOIN Book b ON bc.Book_ID = b.Book_ID
WHERE (b.Book_ID = ? OR b.Title LIKE ? OR b.ISBN = ?)
  AND bc.Status = 'Available'
ORDER BY bc.Book_ID, bc.Copy_ID;

-- S3. Search members by Member ID, Full Name, Identity Card, or Email
SELECT m.Member_ID, m.Full_Name, m.Email, m.Identity_Card, mc.Type_Name AS Category,
       mc.Borrowing_Limit, m.Membership_Expiry_Date, m.Account_Status
FROM Member m
JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
WHERE m.Member_ID LIKE ? OR m.Full_Name LIKE ? OR m.Identity_Card LIKE ? OR m.Email LIKE ?
ORDER BY m.Member_ID ASC;

-- S4. Topic Q2: List all overdue active loans of a specific member
SELECT ldf.Loan_ID, ldf.Detail_ID, m.Member_ID, m.Full_Name AS Member_Name,
       b.Title AS Book_Title, ldf.Copy_ID, lt.Borrow_Date, ldf.Due_Date, ldf.Days_Late, ldf.Fine_Amount
FROM Loan_Detail_Fine ldf
JOIN Loan_Transaction lt ON ldf.Loan_ID = lt.Loan_ID
JOIN Member m ON ldf.Member_ID = m.Member_ID
JOIN Book b ON ldf.Book_ID = b.Book_ID
WHERE (m.Member_ID = ? OR m.Full_Name LIKE ?)
  AND ldf.Return_Date IS NULL
  AND ldf.Due_Date < CURRENT_DATE
ORDER BY ldf.Due_Date ASC;


-- ============================================================================
-- SCREEN 8: ReportView.fxml (Controller: ReportManage - Reports & Analytics)
-- ============================================================================
-- RP1. Topic Q3: Find the top 10 most borrowed book titles in the current year (Category omitted)
SELECT b.Book_ID, b.Title, COUNT(ld.Detail_ID) AS Total_Borrows
FROM Book b
JOIN Loan_Detail ld ON b.Book_ID = ld.Book_ID
JOIN Loan_Transaction lt ON ld.Loan_ID = lt.Loan_ID
WHERE YEAR(lt.Borrow_Date) = YEAR(CURRENT_DATE)
GROUP BY b.Book_ID, b.Title
ORDER BY Total_Borrows DESC
LIMIT 10;

-- RP2. Topic Q5: Calculate total unpaid fines outstanding per member category
SELECT mc.Member_Type_ID, mc.Type_Name,
       COUNT(ldf.Detail_ID) AS Unpaid_Count,
       COALESCE(SUM(ldf.Fine_Amount), 0) AS Total_Outstanding_Fines
FROM Member_Category mc
LEFT JOIN Member m ON mc.Member_Type_ID = m.Member_Type_ID
LEFT JOIN Loan_Detail_Fine ldf ON m.Member_ID = ldf.Member_ID AND ldf.Payment_Status = 'Unpaid'
GROUP BY mc.Member_Type_ID, mc.Type_Name
ORDER BY Total_Outstanding_Fines DESC;

-- RP3. Topic Q4 (Relational Division): Find members who have borrowed every title by a given author
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

-- RP4. Monthly borrowing statistics (total transactions and borrowed copies per month in the current year)
SELECT MONTH(lt.Borrow_Date) AS Borrow_Month,
       COUNT(DISTINCT lt.Loan_ID) AS Total_Transactions,
       COUNT(ld.Detail_ID) AS Total_Copies_Borrowed
FROM Loan_Transaction lt
LEFT JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
WHERE YEAR(lt.Borrow_Date) = YEAR(CURRENT_DATE)
GROUP BY MONTH(lt.Borrow_Date)
ORDER BY Borrow_Month ASC;

-- =====================================================================
-- SECTION 2: DATA MODIFICATION & TRANSACTIONS (REQUIREMENT 2.3)
-- Covers at least one INSERT, UPDATE, and DELETE for each of the 11
-- main entity tables (plus associative/multivalued tables), and
-- multi-table atomic transactions demonstrating COMMIT and ROLLBACK.
-- =====================================================================

-- ---------------------------------------------------------------------
-- [2.3.1] Main Table 1: Category (Category Screen)
-- ---------------------------------------------------------------------
-- INSERT: Add a new subject category
INSERT INTO Category (Category_ID, Category_Name, Description)
VALUES ('CAT16', 'Data Science', 'Big data analytics, data mining, and visualization.');

-- UPDATE: Edit the description of the subject category
UPDATE Category
SET Description = 'Big data analytics, statistical modeling, and visualization.'
WHERE Category_ID = 'CAT16';

-- DELETE: Remove a subject category not referenced by any book (fk_book_category is RESTRICT)
DELETE FROM Category
WHERE Category_ID = 'CAT16';


-- ---------------------------------------------------------------------
-- [2.3.2] Main Table 2: Publisher (Publisher Screen)
-- ---------------------------------------------------------------------
-- INSERT: Add a new publisher
INSERT INTO Publisher (Publisher_ID, Publisher_Name, Address, Contact_Email)
VALUES ('PUB16', 'Addison-Wesley', 'Boston, MA, USA', 'contact@aw.com');

-- UPDATE: Correct the publisher address
UPDATE Publisher
SET Address = 'Boston, Massachusetts, USA'
WHERE Publisher_ID = 'PUB16';

-- DELETE: Remove a publisher with no published titles (fk_book_publisher is RESTRICT)
DELETE FROM Publisher
WHERE Publisher_ID = 'PUB16';


-- ---------------------------------------------------------------------
-- [2.3.3] Main Table 3: Author (Author Screen)
-- ---------------------------------------------------------------------
-- INSERT: Add a new author
INSERT INTO Author (Author_ID, Author_Name, Biography)
VALUES ('AUTH19', 'Andrew S. Tanenbaum', 'Professor Emeritus of Computer Science at Vrije Universiteit Amsterdam.');

-- UPDATE: Edit the author biography
UPDATE Author
SET Biography = 'Author of Modern Operating Systems and Computer Networks.'
WHERE Author_ID = 'AUTH19';

-- DELETE: Remove an author not linked to any title (fk_ba_author is RESTRICT)
DELETE FROM Author
WHERE Author_ID = 'AUTH19';


-- ---------------------------------------------------------------------
-- [2.3.4] Main Table 4: Book & Book_Author (Book Screen)
-- ---------------------------------------------------------------------
-- INSERT: Catalogue a new book title and assign ordered authors
INSERT INTO Book (Book_ID, Category_ID, Publisher_ID, ISBN, Title, Publication_Year)
VALUES ('B16', 'CAT01', 'PUB01', '978-0262046305', 'Introduction to Algorithms, 4th Edition', 2022);

INSERT INTO Book_Author (Book_ID, Author_ID, Author_Order)
VALUES ('B16', 'AUTH01', 1),
       ('B16', 'AUTH16', 3);

-- UPDATE: Correct the book title and reorder author position
UPDATE Book
SET Title = 'Introduction to Algorithms, Fourth Edition'
WHERE Book_ID = 'B16';

UPDATE Book_Author
SET Author_Order = 2
WHERE Book_ID = 'B16' AND Author_ID = 'AUTH16';

-- DELETE: Unlink secondary author, then remove the book (fk_ba_book cascades remaining link)
DELETE FROM Book_Author
WHERE Book_ID = 'B16' AND Author_ID = 'AUTH16';

DELETE FROM Book
WHERE Book_ID = 'B16';


-- ---------------------------------------------------------------------
-- [2.3.5] Main Table 5: Book_Copy (Copy Screen)
-- ---------------------------------------------------------------------
-- INSERT: Add a second physical copy of an existing title (B15)
INSERT INTO Book_Copy (Book_ID, Copy_ID, Shelf_Location, Status, Condition_Notes)
VALUES ('B15', 'C02', 'Shelf G - Row 2', 'Available', 'New');

-- UPDATE: Record physical damage found during shelf inspection
UPDATE Book_Copy
SET Status = 'Damaged', Condition_Notes = 'Damaged: water damage on the cover'
WHERE Book_ID = 'B15' AND Copy_ID = 'C02';

-- DELETE: Write off a copy that has never been borrowed (FK_loandetail_borrowed_BookCopy is RESTRICT)
DELETE FROM Book_Copy
WHERE Book_ID = 'B15' AND Copy_ID = 'C02';


-- ---------------------------------------------------------------------
-- [2.3.6] Main Table 6: Member_Category (Membership Category Screen)
-- ---------------------------------------------------------------------
-- INSERT: Add a new membership tier
INSERT INTO Member_Category (Member_Type_ID, Type_Name, Borrowing_Limit, Loan_Period)
VALUES ('MC16', 'Short-Term Research Visitor', 4, 14);

-- UPDATE: Adjust borrowing limit and loan duration
UPDATE Member_Category
SET Borrowing_Limit = 5, Loan_Period = 17
WHERE Member_Type_ID = 'MC16';

-- DELETE: Remove a membership category with no assigned members (FK_Memberbelongto_category is RESTRICT)
DELETE FROM Member_Category
WHERE Member_Type_ID = 'MC16';


-- ---------------------------------------------------------------------
-- [2.3.7] Main Table 7: Member & Member_Phone (Member Screen)
-- ---------------------------------------------------------------------
-- INSERT: Register a new library member and record contact phone
INSERT INTO Member (Member_ID, Member_Type_ID, Full_Name, Email, Identity_Card, Membership_Expiry_Date, Account_Status)
VALUES ('M16', 'MC01', 'Nguyen Minh Tri', 'nmtri@hcmiu.edu.vn', '079205001016', '2027-08-31', 'Active');

INSERT INTO Member_Phone (Member_ID, Phone)
VALUES ('M16', '0909998816');

-- UPDATE: Renew membership expiry date and update phone number
UPDATE Member
SET Membership_Expiry_Date = '2028-08-31'
WHERE Member_ID = 'M16';

UPDATE Member_Phone
SET Phone = '0909998877'
WHERE Member_ID = 'M16' AND Phone = '0909998816';

-- DELETE: Remove child phone record first, then member with no loan history (FK_loantransactionMakeMember is RESTRICT)
DELETE FROM Member_Phone
WHERE Member_ID = 'M16' AND Phone = '0909998877';

DELETE FROM Member
WHERE Member_ID = 'M16';


-- ---------------------------------------------------------------------
-- [2.3.8] Main Table 8: Staff (Staff Screen)
-- ---------------------------------------------------------------------
-- INSERT: Add a staff member reporting to Head of Circulation (S03)
INSERT INTO Staff (Staff_ID, Supervisor_ID, Full_Name, Position, Email)
VALUES ('S16', 'S03', 'Pham Hoang Nam', 'Assistant Circulation Librarian', 'phnam@hcmiu.edu.vn');

-- UPDATE: Reassign role and supervisor (Trigger 5 prevents self-supervision)
UPDATE Staff
SET Position = 'Reference Librarian', Supervisor_ID = 'S02'
WHERE Staff_ID = 'S16';

-- DELETE: Remove a staff record that has handled no loans (FK_loanhandlesStaffID is RESTRICT)
DELETE FROM Staff
WHERE Staff_ID = 'S16';


-- ---------------------------------------------------------------------
-- [2.3.9] Main Table 9: Reservation (Reservation Screen)
-- ---------------------------------------------------------------------
-- INSERT: Place a reservation for member M02 on fully loaned book B12
INSERT INTO Reservation (Reservation_ID, Member_ID, Book_ID, Reservation_Date, Status)
VALUES ('RES16', 'M02', 'B12', '2026-10-23', 'Pending');

-- UPDATE: Mark reservation as cancelled
UPDATE Reservation
SET Status = 'Cancelled'
WHERE Reservation_ID = 'RES16';

-- DELETE: Delete the cancelled reservation record
DELETE FROM Reservation
WHERE Reservation_ID = 'RES16';


-- ---------------------------------------------------------------------
-- [2.3.10] Main Tables 10 & 11: Loan_Transaction & Loan_Detail
--          (Checkout and Return Screen — Master-Detail)
-- ---------------------------------------------------------------------
-- INSERT: Create a loan header and detail line (validated by Trigger 1 & Trigger 2)
INSERT INTO Loan_Transaction (Loan_ID, Member_ID, Staff_ID, Borrow_Date, Due_Date)
VALUES ('L16', 'M04', 'S05', '2026-10-20', '2026-10-30');

INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID, Return_Date, Renewal_Count, Payment_Status)
VALUES ('L16', 'D01', 'B14', 'C01', NULL, 0, 'None');

-- UPDATE: Renew the loan once and extend the due date
UPDATE Loan_Detail
SET Renewal_Count = 1
WHERE Loan_ID = 'L16' AND Detail_ID = 'D01';

UPDATE Loan_Transaction
SET Due_Date = '2026-11-09'
WHERE Loan_ID = 'L16';

-- UPDATE: Record on-time book return (validated by Trigger 3)
UPDATE Loan_Detail
SET Return_Date = '2026-11-05'
WHERE Loan_ID = 'L16' AND Detail_ID = 'D01';

-- DELETE: Remove the demo loan (child detail line first, then parent header)
DELETE FROM Loan_Detail
WHERE Loan_ID = 'L16' AND Detail_ID = 'D01';

DELETE FROM Loan_Transaction
WHERE Loan_ID = 'L16';


-- =====================================================================
-- [2.3.T] TRANSACTIONS (COMMIT & ROLLBACK)
-- Ensures atomicity when updating multiple related tables together.
-- =====================================================================

-- [2.3.T1] TRANSACTION 1 (COMMIT): Atomic Book Checkout Across 3 Tables
START TRANSACTION;

INSERT INTO Loan_Transaction (Loan_ID, Member_ID, Staff_ID, Borrow_Date, Due_Date)
VALUES ('L17', 'M04', 'S05', '2026-10-20', '2026-10-30');

INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID, Return_Date, Renewal_Count, Payment_Status)
VALUES ('L17', 'D01', 'B14', 'C01', NULL, 0, 'None');

UPDATE Book_Copy
SET Status = 'Borrowed'
WHERE Book_ID = 'B14' AND Copy_ID = 'C01';

COMMIT;


-- [2.3.T2] TRANSACTION 2 (ROLLBACK): Aborted Return & Fine Payment on L03
START TRANSACTION;

-- 1. Record the return date
UPDATE Loan_Detail
SET Return_Date = '2026-10-23'
WHERE Loan_ID = 'L03' AND Detail_ID = 'D01' AND Return_Date IS NULL;

-- 2. Mark fine as paid
UPDATE Loan_Detail
SET Payment_Status = 'Paid'
WHERE Loan_ID = 'L03' AND Detail_ID = 'D01';

-- 3. Release the physical copy back to Available
UPDATE Book_Copy
SET Status = 'Available'
WHERE Book_ID = 'B02' AND Copy_ID = 'C02';

-- 4. Simulate payment gateway failure -> rollback all changes across both tables
ROLLBACK;


-- Cleanup baseline state for ('B14', 'C01') and L17 (child detail first, then parent header)
START TRANSACTION;

DELETE FROM Loan_Detail
WHERE Loan_ID = 'L17' AND Detail_ID = 'D01';

DELETE FROM Loan_Transaction
WHERE Loan_ID = 'L17';

UPDATE Book_Copy
SET Status = 'Available'
WHERE Book_ID = 'B14' AND Copy_ID = 'C01';

COMMIT;