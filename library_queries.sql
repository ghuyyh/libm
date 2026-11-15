-- ============================================================================
-- PROJECT:     IT079IU - PRINCIPLES OF DATABASE MANAGEMENT (GROUP 4)
-- TOPIC 22:    LIBRARY MANAGEMENT SYSTEM
-- DATABASE:    library_management_db (MySQL 8.4 LTS)
-- AUTHORS:     Nguyen Ngoc Phuong Nam & Nguyen Duc Thinh (Query Developers)
-- ============================================================================
-- [QUAN TRỌNG - LƯU Ý CHUNG CHO CẢ NHÓM & NHÓM JAVA DEVELOPER]
-- 1. Cột dẫn xuất đã tách sang View:
--    - Không gọi cột `Total_Copies_Borrowed` trong `Loan_Transaction` -> dùng View `Loan_Summary`.
--    - Không gọi cột `Fine_Amount` hay `Days_Late` trong `Loan_Detail` -> dùng View `Loan_Detail_Fine`.
-- 2. Định dạng mã dữ liệu mẫu trong DB: `'B01'`, `'M01'`, `'AUTH01'`, `'S01'`, `'L01'`, `'C01'`, `'RES01'`.
-- 3. Quy tắc Trigger 3 (Trả sách):
--    - Khi cập nhật `Return_Date` trong `Loan_Detail`, bắt buộc cập nhật kèm `Payment_Status`:
--      + Nếu trả trễ (`Return_Date > Due_Date`) -> `Payment_Status` phải là `'Unpaid'` (không được để `'None'`).
--      + Nếu trả đúng/trước hạn -> `Payment_Status` bắt buộc là `'None'`.
-- 4. Ràng buộc bảng `Member`:
--    - Khi INSERT thành viên mới trên UI, bắt buộc nhập đủ `Email` (đúng format `@`), `Identity_Card`
--      (9-20 ký tự), và `Membership_Expiry_Date`, nếu thiếu MySQL sẽ báo lỗi CHECK/NOT NULL.
-- 5. Bảo mật & Chấm điểm (Mục 2.4):
--    - Tất cả dấu `?` ở PHẦN 2 phải được nhóm Java truyền qua `PreparedStatement` (cấm cộng chuỗi `+`).
--    - Nhớ test ô tìm kiếm với từ khóa `O'Reilly` (có sẵn trong bảng `Publisher`) để kiểm tra dấu nháy đơn `'`.
-- ============================================================================

USE library_management_db;


-- ############################################################################
-- PHẦN 1: 12 CÂU TRUY VẤN SQL NÂNG CAO (DELIVERABLE D5 - CHẠY TRỰC TIẾP TRÊN WORKBENCH)
-- Lưu ý: Đã khớp 100% dữ liệu mẫu để mọi câu đều trả về kết quả khác rỗng (non-empty).
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
-- PHẦN 2: BỘ SQL DÙNG CHO GIAO DIỆN JAVAFX (DÀNH CHO NHÓM JAVA - PREPAREDSTATEMENT)
-- Lưu ý: Phần này dùng tham số `?` để gắn vào JDBC PreparedStatement, không chạy
--        trực tiếp một lượt trên Workbench.
-- ############################################################################

-- ============================================================================
-- MÀN HÌNH 1: HomeView.fxml (Thống kê nhanh Dashboard - Tùy chọn)
-- ============================================================================
SELECT 
    (SELECT COUNT(*) FROM Book) AS Total_Books,
    (SELECT COUNT(*) FROM Member WHERE Account_Status = 'Active') AS Active_Members,
    (SELECT COUNT(*) FROM Loan_Detail WHERE Return_Date IS NULL) AS Active_Loans,
    (SELECT COUNT(*) FROM Reservation WHERE Status = 'Pending') AS Pending_Reservations,
    (SELECT COALESCE(SUM(Fine_Amount), 0) FROM Loan_Detail_Fine WHERE Payment_Status = 'Unpaid') AS Unpaid_Fines;


-- ============================================================================
-- MÀN HÌNH 2: BookView.fxml (Quản lý Sách & Bản sao vật lý - Master-Detail + CRUD)
-- ============================================================================
-- B1. Load danh sách tất cả đầu sách (kèm Thể loại, NXB, Tác giả, số bản sao Available)
SELECT b.Book_ID, b.ISBN, b.Title, b.Publication_Year,
       b.Category_ID, c.Category_Name,
       b.Publisher_ID, p.Publisher_Name,
       GROUP_CONCAT(DISTINCT a.Author_Name ORDER BY ba.Author_Order SEPARATOR ', ') AS Authors,
       COUNT(DISTINCT CASE WHEN bc.Status = 'Available' THEN bc.Copy_ID END) AS Available_Copies
FROM Book b
JOIN Category c ON b.Category_ID = c.Category_ID
JOIN Publisher p ON b.Publisher_ID = p.Publisher_ID
LEFT JOIN Book_Author ba ON b.Book_ID = ba.Book_ID
LEFT JOIN Author a ON ba.Author_ID = a.Author_ID
LEFT JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID
GROUP BY b.Book_ID, b.ISBN, b.Title, b.Publication_Year, b.Category_ID, c.Category_Name, b.Publisher_ID, p.Publisher_Name
ORDER BY b.Book_ID ASC;

-- B2. Master-Detail: Bấm chọn 1 tựa sách ở bảng trên -> Load danh sách bản sao ở bảng dưới
SELECT Copy_ID, Shelf_Location, Status, Condition_Notes
FROM Book_Copy
WHERE Book_ID = ?
ORDER BY Copy_ID ASC;

-- B3. Thêm / Sửa / Xóa Đầu sách (Book)
INSERT INTO Book (Book_ID, Category_ID, Publisher_ID, ISBN, Title, Publication_Year) VALUES (?, ?, ?, ?, ?, ?);
UPDATE Book SET Category_ID = ?, Publisher_ID = ?, ISBN = ?, Title = ?, Publication_Year = ? WHERE Book_ID = ?;
DELETE FROM Book WHERE Book_ID = ?;

-- B4. Thêm / Sửa / Xóa Bản sao sách (Book_Copy)
INSERT INTO Book_Copy (Book_ID, Copy_ID, Shelf_Location, Status, Condition_Notes) VALUES (?, ?, ?, ?, ?);
UPDATE Book_Copy SET Shelf_Location = ?, Status = ?, Condition_Notes = ? WHERE Book_ID = ? AND Copy_ID = ?;
DELETE FROM Book_Copy WHERE Book_ID = ? AND Copy_ID = ?;


-- ============================================================================
-- MÀN HÌNH 3: MemberView.fxml (Controller: MemberManage)
-- Lưu ý: Alias cột khớp với fx:id trên TableView (col_memid, col_name, col_cat...)
-- ============================================================================
-- M1. Load danh sách độc giả lên bảng `mem_table`
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

-- M2. Load danh sách hạng thẻ vào ChoiceBox `cat_select`
SELECT Member_Type_ID, Type_Name FROM Member_Category ORDER BY Member_Type_ID ASC;

-- M3. Nút `Update` ở khung dưới (Cập nhật Tên & Hạng thẻ)
UPDATE Member
SET Full_Name = ?,
    Member_Type_ID = (SELECT Member_Type_ID FROM Member_Category WHERE Type_Name = ?)
WHERE Member_ID = ?;

-- M4. Nút `Add menber+` (Phải truyền đủ Email, Identity_Card, Membership_Expiry_Date)
INSERT INTO Member (Member_ID, Member_Type_ID, Full_Name, Email, Identity_Card, Membership_Expiry_Date, Account_Status)
VALUES (?, ?, ?, ?, ?, ?, 'Active');

-- M5. Xóa độc giả (ON DELETE RESTRICT sẽ tự báo lỗi nếu độc giả từng mượn sách)
DELETE FROM Member WHERE Member_ID = ?;


-- ============================================================================
-- MÀN HÌNH 4: LoanView.fxml (Quản lý Mượn / Trả sách)
-- Lưu ý: Nhớ đặt fx:id cho TableView, CheckBox, các Label trong Details và 2 nút bấm.
-- ============================================================================
-- L1. Load toàn bộ phiếu mượn lên bảng & khung Details
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

-- L2. CheckBox `Show overdue today` (Lọc các sách chưa trả và đã quá hạn)
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

-- L3. Nút `Returned ✓` (Chạy 2 lệnh dưới đây trong cùng 1 Java Transaction)
UPDATE Loan_Detail ld
JOIN Loan_Transaction lt ON ld.Loan_ID = lt.Loan_ID
SET ld.Return_Date = CURRENT_DATE,
    ld.Payment_Status = CASE WHEN CURRENT_DATE > lt.Due_Date THEN 'Unpaid' ELSE 'None' END
WHERE ld.Loan_ID = ? AND ld.Detail_ID = ? AND ld.Return_Date IS NULL;

UPDATE Book_Copy SET Status = 'Available' WHERE Book_ID = ? AND Copy_ID = ?;

-- L4. Nút `New loan+` (Chạy 3 lệnh dưới đây trong cùng 1 Java Transaction)
INSERT INTO Loan_Transaction (Loan_ID, Member_ID, Staff_ID, Borrow_Date, Due_Date) VALUES (?, ?, ?, CURRENT_DATE, ?);
INSERT INTO Loan_Detail (Loan_ID, Detail_ID, Book_ID, Copy_ID, Return_Date, Renewal_Count, Payment_Status) VALUES (?, ?, ?, ?, NULL, 0, 'None');
UPDATE Book_Copy SET Status = 'Borrowed' WHERE Book_ID = ? AND Copy_ID = ?;


-- ============================================================================
-- MÀN HÌNH 5: ReservationView.fxml (Controller: ReservationManage)
-- ============================================================================
-- RS1. Load danh sách đặt trước
SELECT r.Reservation_ID, r.Member_ID, m.Full_Name AS Member_Name,
       r.Book_ID, b.Title AS Book_Title, r.Reservation_Date, r.Status,
       (SELECT COUNT(*) FROM Book_Copy bc WHERE bc.Book_ID = r.Book_ID AND bc.Status = 'Available') AS Available_Copies
FROM Reservation r
JOIN Member m ON r.Member_ID = m.Member_ID
JOIN Book b ON r.Book_ID = b.Book_ID
ORDER BY r.Reservation_Date ASC;

-- RS2. Tạo đặt trước mới / Cập nhật trạng thái / Xóa đặt trước
INSERT INTO Reservation (Reservation_ID, Member_ID, Book_ID, Reservation_Date, Status) VALUES (?, ?, ?, CURRENT_DATE, 'Pending');
UPDATE Reservation SET Status = ? WHERE Reservation_ID = ?;
DELETE FROM Reservation WHERE Reservation_ID = ?;


-- ============================================================================
-- MÀN HÌNH 6: FineView.fxml (Quản lý Tiền phạt - dùng View Loan_Detail_Fine)
-- ============================================================================
-- F1. Load danh sách các khoản phạt (lọc theo Payment_Status nếu cần: 'Unpaid', 'Paid', 'Waived')
SELECT ldf.Loan_ID, ldf.Detail_ID, ldf.Member_ID, m.Full_Name AS Member_Name,
       b.Title AS Book_Title, ldf.Copy_ID, ldf.Due_Date, ldf.Return_Date,
       ldf.Days_Late, ldf.Fine_Amount, ldf.Payment_Status
FROM Loan_Detail_Fine ldf
JOIN Member m ON ldf.Member_ID = m.Member_ID
JOIN Book b ON ldf.Book_ID = b.Book_ID
WHERE ldf.Fine_Amount > 0
ORDER BY ldf.Payment_Status DESC, ldf.Due_Date ASC;

-- F2. Xác nhận đóng phạt ('Paid') hoặc miễn phạt ('Waived')
UPDATE Loan_Detail
SET Payment_Status = ?
WHERE Loan_ID = ? AND Detail_ID = ?;


-- ============================================================================
-- MÀN HÌNH 7: SearchView.fxml (Tìm kiếm tổng hợp - Truyền "%" + keyword + "%" cho LIKE)
-- ============================================================================
-- S1. Tìm Sách theo Mã, Tên, ISBN, Tác giả hoặc Thể loại
SELECT b.Book_ID, b.ISBN, b.Title, b.Publication_Year, c.Category_Name, p.Publisher_Name,
       GROUP_CONCAT(DISTINCT a.Author_Name ORDER BY ba.Author_Order SEPARATOR ', ') AS Authors,
       COUNT(DISTINCT CASE WHEN bc.Status = 'Available' THEN bc.Copy_ID END) AS Available_Copies
FROM Book b
JOIN Category c ON b.Category_ID = c.Category_ID
JOIN Publisher p ON b.Publisher_ID = p.Publisher_ID
LEFT JOIN Book_Author ba ON b.Book_ID = ba.Book_ID
LEFT JOIN Author a ON ba.Author_ID = a.Author_ID
LEFT JOIN Book_Copy bc ON b.Book_ID = bc.Book_ID
WHERE b.Book_ID LIKE ? OR b.Title LIKE ? OR b.ISBN LIKE ? OR a.Author_Name LIKE ? OR c.Category_Name LIKE ?
GROUP BY b.Book_ID, b.ISBN, b.Title, b.Publication_Year, c.Category_Name, p.Publisher_Name
ORDER BY b.Title ASC;

-- S2. Topic Q1: Tìm các bản sao đang Available của một tựa sách
SELECT bc.Book_ID, b.Title, b.ISBN, bc.Copy_ID, bc.Shelf_Location, bc.Status, bc.Condition_Notes
FROM Book_Copy bc
JOIN Book b ON bc.Book_ID = b.Book_ID
WHERE (b.Book_ID = ? OR b.Title LIKE ? OR b.ISBN = ?)
  AND bc.Status = 'Available'
ORDER BY bc.Book_ID, bc.Copy_ID;

-- S3. Tìm Độc giả theo Mã, Họ tên, CCCD hoặc Email
SELECT m.Member_ID, m.Full_Name, m.Email, m.Identity_Card, mc.Type_Name AS Category,
       mc.Borrowing_Limit, m.Membership_Expiry_Date, m.Account_Status
FROM Member m
JOIN Member_Category mc ON m.Member_Type_ID = mc.Member_Type_ID
WHERE m.Member_ID LIKE ? OR m.Full_Name LIKE ? OR m.Identity_Card LIKE ? OR m.Email LIKE ?
ORDER BY m.Member_ID ASC;

-- S4. Topic Q2: Tra cứu sách quá hạn của một độc giả cụ thể
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
-- MÀN HÌNH 8: ReportView.fxml (Controller: ReportManage - Báo cáo & Thống kê)
-- ============================================================================
-- RP1. Topic Q3: Top 10 tựa sách được mượn nhiều nhất trong năm nay
SELECT b.Book_ID, b.Title, c.Category_Name, COUNT(ld.Detail_ID) AS Total_Borrows
FROM Book b
JOIN Category c ON b.Category_ID = c.Category_ID
JOIN Loan_Detail ld ON b.Book_ID = ld.Book_ID
JOIN Loan_Transaction lt ON ld.Loan_ID = lt.Loan_ID
WHERE YEAR(lt.Borrow_Date) = YEAR(CURRENT_DATE)
GROUP BY b.Book_ID, b.Title, c.Category_Name
ORDER BY Total_Borrows DESC
LIMIT 10;

-- RP2. Topic Q5: Tổng tiền phạt chưa thanh toán theo từng loại thành viên
SELECT mc.Member_Type_ID, mc.Type_Name,
       COUNT(ldf.Detail_ID) AS Unpaid_Count,
       COALESCE(SUM(ldf.Fine_Amount), 0) AS Total_Outstanding_Fines
FROM Member_Category mc
LEFT JOIN Member m ON mc.Member_Type_ID = m.Member_Type_ID
LEFT JOIN Loan_Detail_Fine ldf ON m.Member_ID = ldf.Member_ID AND ldf.Payment_Status = 'Unpaid'
GROUP BY mc.Member_Type_ID, mc.Type_Name
ORDER BY Total_Outstanding_Fines DESC;

-- RP3. Topic Q4 (Relational Division): Độc giả đã mượn tất cả tựa sách của 1 tác giả
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

-- RP4. Thống kê lượt mượn theo từng tháng trong năm
SELECT MONTH(lt.Borrow_Date) AS Borrow_Month,
       COUNT(DISTINCT lt.Loan_ID) AS Total_Transactions,
       COUNT(ld.Detail_ID) AS Total_Copies_Borrowed
FROM Loan_Transaction lt
LEFT JOIN Loan_Detail ld ON lt.Loan_ID = ld.Loan_ID
WHERE YEAR(lt.Borrow_Date) = YEAR(CURRENT_DATE)
GROUP BY MONTH(lt.Borrow_Date)
ORDER BY Borrow_Month ASC;