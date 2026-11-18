package com.pdm.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.pdm.dao.LoanDAO;
import com.pdm.model.Loan;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class LoanManage extends basecontrol {

    // ----- Bảng danh sách -----
    @FXML
    private TableView<Loan> loan_table;
    @FXML
    private TableColumn<Loan, String> col_loanid;
    @FXML
    private TableColumn<Loan, String> col_memname;
    @FXML
    private TableColumn<Loan, String> col_title;
    @FXML
    private TableColumn<Loan, String> col_copyid;
    @FXML
    private TableColumn<Loan, String> col_datedue;
    @FXML
    private TableColumn<Loan, String> col_status;

    @FXML
    private TextField search_field;
    @FXML
    private CheckBox overdue_check;

    // ----- Tab Details & Return -----
    @FXML
    private Label lbl_loanid;
    @FXML
    private Label lbl_memid;
    @FXML
    private Label lbl_memname;
    @FXML
    private Label lbl_isbn;
    @FXML
    private Label lbl_title;
    @FXML
    private Label lbl_dateout;
    @FXML
    private Label lbl_datereturn;
    @FXML
    private Label lbl_status;
    @FXML
    private Button btn_return;

    // ----- Tab New Borrowing -----
    @FXML
    private TextField new_memid;
    @FXML
    private TextField new_isbn;
    @FXML
    private TextField new_staff;

    private final LoanDAO loanDAO = new LoanDAO();
    private final ObservableList<Loan> loans = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        col_loanid.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLoan_ID()));
        col_memname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMemberName()));
        col_title.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        col_copyid.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCopyId()));
        col_datedue.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDueDate())));
        col_status.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        loan_table.setItems(loans);
        loan_table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldLoan, newLoan) -> showDetails(newLoan));

        showDetails(null);
        loadLoans();
    }

    // ------------------------------------------------------------------
    // Nạp dữ liệu lên bảng (có tính đến ô tìm kiếm và ô overdue)
    // ------------------------------------------------------------------
    private void loadLoans() {
        try {
            String keyword = search_field.getText() == null ? "" : search_field.getText().trim();
            List<Loan> result;
            if (keyword.isEmpty()) {
                result = overdue_check.isSelected() ? loanDAO.getOverdueLoans() : loanDAO.getAllLoans();
            } else {
                result = loanDAO.searchLoans(keyword);
                if (overdue_check.isSelected()) {
                    result.removeIf(l -> !"Overdue".equals(l.getStatus()));
                }
            }
            loans.setAll(result);
        } catch (SQLException e) {
            showError("Cannot load loans", e.getMessage());
        }
    }

    // Nhấn Enter trong ô tìm kiếm (ghi đè hàm rỗng của basecontrol)
    @Override
    @FXML
    public void search() {
        loadLoans();
    }

    // Tick / bỏ tick "Show overdue today"
    @FXML
    public void overdueToggle() {
        loadLoans();
    }

    // ------------------------------------------------------------------
    // Hiện chi tiết phiếu mượn đang chọn
    // ------------------------------------------------------------------
    private void showDetails(Loan l) {
        if (l == null) {
            lbl_loanid.setText("Loan ID:");
            lbl_memid.setText("Member ID:");
            lbl_memname.setText("Member Name:");
            lbl_isbn.setText("ISBN:");
            lbl_title.setText("Title:");
            lbl_dateout.setText("Date Out:");
            lbl_datereturn.setText("Date Return:");
            lbl_status.setText("Status:");
            btn_return.setDisable(true);
            return;
        }
        lbl_loanid.setText("Loan ID: " + l.getLoan_ID());
        lbl_memid.setText("Member ID: " + l.getMemberId());
        lbl_memname.setText("Member Name: " + l.getMemberName());
        lbl_isbn.setText("ISBN: " + l.getIsbn());
        lbl_title.setText("Title: " + l.getTitle());
        lbl_dateout.setText("Date Out: " + l.getBorrowDate());
        lbl_datereturn.setText("Date Return: " + (l.getReturnDate() == null ? "Not returned yet" : l.getReturnDate()));
        String status = l.getStatus();
        if ("Unpaid".equals(l.getPaymentStatus())) {
            status += " (fine unpaid)";
        }
        lbl_status.setText("Status: " + status);
        btn_return.setDisable(l.getReturnDate() != null);
    }

    // ------------------------------------------------------------------
    // Nút "Returned ✓"
    // ------------------------------------------------------------------
    @FXML
    public void returnClick() {
        Loan selected = loan_table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select a borrowing in the table first.");
            return;
        }
        if (selected.getReturnDate() != null) {
            showInfo("This book has already been returned.");
            return;
        }
        try {
            boolean late = selected.getDueDate() != null && selected.getDueDate().isBefore(LocalDate.now());
            loanDAO.returnBook(selected.getLoan_ID(), selected.getDetailId());
            loadLoans();
            showInfo(late ? "Book returned LATE. A fine has been recorded (see Fines)."
                    : "Book returned on time.");
        } catch (SQLException e) {
            showError("Cannot return this book", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Nút "New loan+" ở tab New Borrowing
    // ------------------------------------------------------------------
    @FXML
    public void addLoanClick() {
        String memberId = new_memid.getText() == null ? "" : new_memid.getText().trim();
        String isbn = new_isbn.getText() == null ? "" : new_isbn.getText().trim();
        String staffId = new_staff.getText() == null ? "" : new_staff.getText().trim();

        if (memberId.isEmpty() || isbn.isEmpty() || staffId.isEmpty()) {
            showError("Missing information", "Please fill in Member ID, ISBN and Staff ID.");
            return;
        }
        try {
            String bookId = loanDAO.findBookIdByIsbn(isbn);
            if (bookId == null) {
                showError("Book not found", "No book with ISBN " + isbn + ".");
                return;
            }
            String copyId = loanDAO.findAvailableCopy(bookId);
            if (copyId == null) {
                showError("No copy available", "All copies of this book are currently out.");
                return;
            }
            String loanId = loanDAO.addLoan(memberId, staffId, bookId, copyId);
            new_memid.clear();
            new_isbn.clear();
            loadLoans();
            showInfo("Loan " + loanId + " created (copy " + copyId + ").");
        } catch (SQLException e) {
            // Trigger của DB (thành viên bị khóa, mượn quá giới hạn...) báo lỗi ở đây
            showError("Cannot create loan", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loan Management");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Loan Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}