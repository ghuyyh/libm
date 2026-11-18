package com.pdm.model;

import java.time.LocalDate;

public class Loan {
    private String Loan_ID;
    private String detailId;
    private String MemberId;
    private String memberName;
    private String bookId;
    private String isbn;
    private String title;
    private String copyId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;   // null nếu chưa trả
    private String paymentStatus;   // 'None', 'Unpaid', 'Paid', 'Waived'

    public Loan() {
    }

    public Loan(String Loan_ID, String detailId, String MemberId, String memberName,
                String bookId, String isbn, String title, String copyId,
                LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
                String paymentStatus) {
        this.Loan_ID = Loan_ID;
        this.detailId = detailId;
        this.MemberId = MemberId;
        this.memberName = memberName;
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.copyId = copyId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.paymentStatus = paymentStatus;
    }

    public String getLoan_ID() { return Loan_ID; }
    public String getDetailId() { return detailId; }
    public String getMemberId() { return MemberId; }
    public String getMemberName() { return memberName; }
    public String getBookId() { return bookId; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getCopyId() { return copyId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getPaymentStatus() { return paymentStatus; }

    /** Trạng thái hiển thị trên bảng, tự tính từ ngày. */
    public String getStatus() {
        if (returnDate != null) {
            return "Returned";
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return "Overdue";
        }
        return "Borrowed";
    }
}