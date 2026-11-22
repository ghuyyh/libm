# Library Management System — Database Design (Topic 22)

Database `library_management_db` · MySQL 8.4 LTS · InnoDB · IT079IU (HCMIU), Group 4.

Contents: [1. Tables](#1-tables) · [2. Business rules](#2-business-rules) ·
[3. Indexes](#3-indexes) · [4. Triggers](#4-triggers)

---

## 1. Tables

The database has 13 tables and 2 views. Classification used below:

- **Strong (simple)**: the table has a primary key of its own.
- **Weak**: the table cannot identify its rows on its own; its primary key contains the key of its owner table.
- **M:N link** and **multivalued attribute table**: tables split off during the ER-to-relational mapping.

Notation: (PK) primary key, (FK) foreign key.

| # | Table | Kind | Primary key | Foreign keys (references) | Attributes |
|---|---|---|---|---|---|
| 1 | Member_Category | Strong (simple) | `Member_Type_ID` | — | Member_Type_ID (PK), Type_Name, Borrowing_Limit, Loan_Period |
| 2 | Member | Strong (simple) | `Member_ID` | Member_Type_ID → Member_Category(Member_Type_ID) | Member_ID (PK), Member_Type_ID (FK), Full_Name, Email, Identity_Card, Membership_Expiry_Date, Account_Status |
| 3 | Member_Phone | Multivalued attribute table (Phone of Member) | `(Member_ID, Phone)` | Member_ID → Member(Member_ID) | Member_ID (PK, FK), Phone (PK) |
| 4 | Category | Strong (simple) | `Category_ID` | — | Category_ID (PK), Category_Name, Description |
| 5 | Publisher | Strong (simple) | `Publisher_ID` | — | Publisher_ID (PK), Publisher_Name, Address, Contact_Email |
| 6 | Author | Strong (simple) | `Author_ID` | — | Author_ID (PK), Author_Name, Biography |
| 7 | Book | Strong (simple) | `Book_ID` | Category_ID → Category(Category_ID); Publisher_ID → Publisher(Publisher_ID) | Book_ID (PK), Category_ID (FK, default `'CAT00'`), Publisher_ID (FK), ISBN, Title, Publication_Year |
| 8 | Book_Author | M:N link (Book – Author) | `(Book_ID, Author_ID)` | Book_ID → Book(Book_ID); Author_ID → Author(Author_ID) | Book_ID (PK, FK), Author_ID (PK, FK), Author_Order |
| 9 | Staff | Strong (simple), with a self-relationship | `Staff_ID` | Supervisor_ID → Staff(Staff_ID) (self-reference) | Staff_ID (PK), Supervisor_ID (FK, may be NULL), Full_Name, Position, Email |
| 10 | Loan_Transaction | Strong (simple) | `Loan_ID` | Member_ID → Member(Member_ID); Staff_ID → Staff(Staff_ID) | Loan_ID (PK), Member_ID (FK), Staff_ID (FK), Borrow_Date, Due_Date |
| 11 | Book_Copy | Weak (owner: Book) | `(Book_ID, Copy_ID)` | Book_ID → Book(Book_ID) | Book_ID (PK, FK), Copy_ID (PK, partial key), Shelf_Location, Status, Condition_Notes |
| 12 | Reservation | Strong (simple) | `Reservation_ID` | Member_ID → Member(Member_ID); Book_ID → Book(Book_ID) | Reservation_ID (PK), Member_ID (FK), Book_ID (FK), Reservation_Date, Status |
| 13 | Loan_Detail | Weak (owner: Loan_Transaction) | `(Loan_ID, Detail_ID)` | Loan_ID → Loan_Transaction(Loan_ID); (Book_ID, Copy_ID) → Book_Copy(Book_ID, Copy_ID) (composite FK) | Loan_ID (PK, FK), Detail_ID (PK, partial key), Book_ID (FK), Copy_ID (FK), Return_Date, Renewal_Count, Payment_Status |

The tables are created in this order, so every foreign key points to a table
that already exists.

**Derived attributes.** The two derived attributes of the ER diagram (dashed
ellipses) are not stored as columns, because their value can be computed from
other data:

| Derived attribute | View | Computed as |
|---|---|---|
| `Loan_Transaction.Total_Copies_Borrowed` | `Loan_Summary` | number of `Loan_Detail` lines of the loan |
| `Loan_Detail.Fine_Amount` | `Loan_Detail_Fine` | 5,000 VND × days the copy was kept after its due date (counted to the return date, or to today while the copy is still out) |

## 2. Business rules

| Rule | Statement |
|---|---|
| BR01 | A physical book copy (Book_Copy) cannot exist independently without being associated with a parent book title (Book). |
| BR02 | Book reservations (Reservation) are placed on a specific Book (title), whereas active loans (Loan_Detail) are tracked by physical book copies (Book_Copy). |
| BR03 | Member borrowing privileges (limits and loan durations) are strictly governed by their membership category (Member_Category). |
| BR04 | Staff members operate under an organizational hierarchy where a staff member may report to a designated supervisor (Staff self-reference). |
| BR05 | Overdue books incur fines, which are tracked for each specific borrowed copy in the Loan_Detail table, along with their payment statuses. |

### Where each rule is enforced

Each rule is enforced in the database by a declarative constraint where one can
express it, and by a trigger (section 4) where it cannot.

| Rule | Declarative constraints | Triggers |
|---|---|---|
| BR01 | `Book_Copy.Book_ID NOT NULL`; PK `(Book_ID, Copy_ID)` contains the key of Book; FK `fk_copy_book` ON DELETE CASCADE (copies go with their title) | — |
| BR02 | FK `fk_res_book`: a reservation references a title and has no copy column; composite FK `FK_loandetail_borrowed_BookCopy`: a loan line references one copy; `unique_loandetail_copy_per_loan`: a copy appears at most once in a loan | 2, 3: the copy must exist, be `Available` or `Reserved`, and be on at most one open loan line |
| BR03 | `Member.Member_Type_ID NOT NULL` + FK `FK_Memberbelongto_category`; CHECK `check_MemberCategoryLimit` (limit 1–20 copies, period 7–20 days) | 1: account Active, card not expired, loan period and borrowing limit respected; 2: limit on each copy taken; 3: limit when a line is re-opened or moved |
| BR04 | FK `FK_staffSupervisor` (Staff → Staff), `Supervisor_ID` may be NULL (top of the hierarchy), ON DELETE SET NULL | 4, 5: nobody supervises themself |
| BR05 | `Payment_Status` in every Loan_Detail row, CHECK `check_loandetail_paymentstatus` (None, Unpaid, Paid, Waived); view `Loan_Detail_Fine` computes the amount | 2, 3: a copy returned late carries a fine status, a copy returned on time carries `None` |
| Dates | CHECK `check_loantransactionDate` (`Due_Date >= Borrow_Date`) | 2, 3: a copy is not returned before it was borrowed |

**Why a reservation is for a title but a loan is of a copy (BR02).** A member
who reserves a book waits for *any* copy of that title, whichever comes back
first, so `Reservation` references `Book`. A loan hands over one physical object
with its own shelf location, condition and history, and that object must come
back, so `Loan_Detail` references `Book_Copy`.

**Rules left to the Java application**, documented as allowed by section 2.3 of
the assignment:

- a title may be reserved only when no copy of it is `Available`;
- a change to an existing loan (new due date, new member) must respect the loan
  period and borrowing limit of the member's category.

## 3. Indexes

The script declares 7 secondary indexes, each serving one of the five Topic 22
queries Q1–Q5.

| # | Index | Table (columns) | Query | Purpose |
|---|---|---|---|---|
| 1 | `index_loanTransaction_member_due` | Loan_Transaction (Member_ID, Due_Date) | Q2 | Finds the loans of one member by due date to detect overdue loans; its leading column Member_ID also serves the foreign key to Member |
| 2 | `idx_loan_detail_return_date` | Loan_Detail (Return_Date) | Q2 | Filters the copies not yet returned (`Return_Date IS NULL`) without a full table scan |
| 3 | `idx_loantransaction_borrow_date` | Loan_Transaction (Borrow_Date) | Q3 | Ten most borrowed titles this year, when the year is written as a date range: `Borrow_Date >= MAKEDATE(YEAR(CURRENT_DATE), 1) AND Borrow_Date < MAKEDATE(YEAR(CURRENT_DATE) + 1, 1)`. A condition such as `YEAR(Borrow_Date) = YEAR(CURRENT_DATE)` wraps the column in a function and cannot use the index |
| 4 | `idx_loan_detail_payment_status` | Loan_Detail (Payment_Status) | Q5 | Filters the loan lines with an unpaid fine before totalling them per member category |
| 5 | `idx_book_title` | Book (Title) | Q1 | Finds a title by name; effective for a prefix search such as `Title LIKE 'Intro%'`, while a pattern that starts with `%` (for example `'%algo%'`) must scan the table |
| 6 | `idx_book_copy_status` | Book_Copy (Book_ID, Status) | Q1 | Counts or lists the available copies (`Available`) of a title |
| 7 | `idx_author_name` | Author (Author_Name) | Q4 | Finds the titles of an author by the author's name |

Besides these 7 indexes, InnoDB indexes every primary key and UNIQUE constraint
(for example `Book.ISBN`, `Member.Email`) and creates an index for any
foreign-key column that has no suitable index, so joins on foreign keys are
supported.

## 4. Triggers

There are 5 triggers, all `BEFORE` triggers, so a violation stops the statement
before any row is written. They enforce rules that cannot be written as a
`CHECK` constraint, for two reasons:

- **Triggers 1–3 read another table.** A MySQL `CHECK` sees only the columns of
  the row being written and may not contain a subquery. The due date lives in
  `Loan_Transaction`, the borrowing limit in `Member_Category`, the open loans in
  `Loan_Detail`.
- **Triggers 4–5 replace a forbidden CHECK.** The rule `Supervisor_ID <> Staff_ID`
  needs only one row, but `Supervisor_ID` has `ON DELETE SET NULL`, and MySQL
  refuses a `CHECK` on a column used by a foreign-key referential action
  (error 3823).

On a violation, a trigger raises `SIGNAL SQLSTATE '45000'` (error 1644) with a
message that starts with the rule code (BR02–BR05), so the Java application can
show it to the user in plain language.

| # | Trigger | Table, event | Rules | Effect |
|---|---|---|---|---|
| 1 | `trigger_loantransaction_before_insertMember` | Loan_Transaction, BEFORE INSERT | BR03 | Refuses a new loan when the account is not `Active`, the card has expired on the borrow date, the loan period exceeds the `Loan_Period` of the member's category, or the copies the member already holds plus one exceed `Borrowing_Limit` |
| 2 | `trg_before_loan_detail_insert_check_copy` | Loan_Detail, BEFORE INSERT | BR02, BR03, BR05 | Refuses a return date earlier than the borrow date; a line returned late must carry a fine status (Unpaid, Paid or Waived) and a line returned on time must be `None`; for an open line, the copy must exist, have status `Available` or `Reserved`, not be on another open line, and the member must still be within the borrowing limit |
| 3 | `trg_before_loan_detail_update_check` | Loan_Detail, BEFORE UPDATE | BR02, BR03, BR05 | Rechecks the date and fine rules when a return is recorded. When a line is re-opened or its copy is changed, it checks that the copy is not on another open line; when a line is re-opened or moved to another loan, it checks the borrowing limit again |
| 4 | `trg_staff_before_insert_hierarchy` | Staff, BEFORE INSERT | BR04 | A new staff member cannot be their own supervisor |
| 5 | `trg_staff_before_update_hierarchy` | Staff, BEFORE UPDATE | BR04 | A staff member's supervisor cannot be changed to themself |

**Why both Trigger 1 and Trigger 2 check the borrowing limit.** Trigger 1 runs
once, when the loan header is created and before any copy is added, so it can
only refuse a new loan to a member who already holds the maximum. Trigger 2 runs
for every copy added, so the limit holds however many copies a loan has and
whether they are added to a new or an existing loan.


| **T2: Cancelled return and fine payment** | `ROLLBACK` | 1) Record the return of `L03-D01`.<br>2) Set `Payment_Status = 'Paid'`.<br>3) Change copy `B02-C02` back to `Available`.<br>4) The payment gateway reports an error, so `ROLLBACK`. | `Loan_Detail`, `Book_Copy` | Demonstrates atomicity: when the payment fails, the book return record, the fine status, and the copy status are all rolled back. |
| **Cleanup** | `COMMIT` | Delete loan slip `L17` (its loan lines are deleted via `CASCADE`) and set `B14-C01` back to `Available`. | `Loan_Transaction`, `Book_Copy` | Restores the sample data to its original state after running T1. |
