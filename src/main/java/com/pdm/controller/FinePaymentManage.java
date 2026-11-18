package com.pdm.controller;

import com.pdm.dao.FineDAO;
import com.pdm.model.Fine;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class FinePaymentManage extends basecontrol {

    @FXML
    private TableView<Fine> fine_table;
    @FXML
    private TableColumn<Fine, String> col_loanid;
    @FXML
    private TableColumn<Fine, String> col_memname;
    @FXML
    private TableColumn<Fine, String> col_title;
    @FXML
    private TableColumn<Fine, String> col_copyid;
    @FXML
    private TableColumn<Fine, String> col_duedate;
    @FXML
    private TableColumn<Fine, String> col_returndate;
    @FXML
    private TableColumn<Fine, String> col_status;
    @FXML
    private TableColumn<Fine, String> col_fine_amount;
    @FXML
    private Label days_late;
    @FXML
    private Label total_fines;
    @FXML
    private Label calculate;

    // @FXML
    // private ChoiceBox<String> status_select = new ChoiceBox<>();

    private ObservableList<Fine> fine_list = FXCollections.observableArrayList();
    private FineDAO fineDAO = new FineDAO();

    @FXML
    public void initialize() {
        // // status_select.getItems().addAll("None", "Waived", "Unpaid");
        // status_select.setValue("None");// de day sua sau

        col_loanid.setCellValueFactory(new PropertyValueFactory<>("Loan_ID"));
        col_memname.setCellValueFactory(new PropertyValueFactory<>("Member_Name"));
        col_title.setCellValueFactory(new PropertyValueFactory<>("Book_Title"));
        col_copyid.setCellValueFactory(new PropertyValueFactory<>("Copy_ID"));
        col_duedate.setCellValueFactory(new PropertyValueFactory<>("Due_Date"));
        col_returndate.setCellValueFactory(new PropertyValueFactory<>("Return_Date"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("Payment_Status"));
        col_fine_amount.setCellValueFactory(new PropertyValueFactory<>("Fine_Amount"));

        fine_table.setItems(fine_list);
        refreshFineTable();
        total_fines.setText("Total Fines: " + fine_list.size());

        fine_table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                days_late.setText("" + newSelection.getDays_Late());
                calculate.setText("= " + "5000 * " + newSelection.getDays_Late() + " = "
                        + (5000 * newSelection.getDays_Late()) + " VND");

            } else {
                refreshFineTable();
                total_fines.setText("Total Fines: " + fine_list.size());
                days_late.setText("");
                calculate.setText("= ");
            }
        });
    }

    public void refreshFineTable() {
        fine_list.setAll(fineDAO.get_fines());
        total_fines.setText("Total Fines: " + fine_list.size());
        days_late.setText("");
        calculate.setText("= ");
    }

    @FXML
    private void processPayment() {
        Fine selectedFine = fine_table.getSelectionModel().getSelectedItem();
        if (selectedFine != null) {
            String loanId = selectedFine.getLoan_ID();
            String status = "Paid";
            String detailId = selectedFine.getDetail_ID();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setContentText("Are you sure you want to process this payment?");
            confirm.showAndWait();
            if (confirm.getResult().getText().equals("Cancel")) {
                return;
            }

            if (selectedFine.getPayment_Status().equals("Paid") || selectedFine.getPayment_Status().equals("Waived")) {
                showError("This fine is already paid. or waived.");
                return;
            }

            boolean updateSuccess = fineDAO.updatePaymentStatus(loanId, status, detailId);
            if (updateSuccess) {
                showError("Payment status updated successfully.");
                refreshFineTable();
            } else {
                showError("Failed to process payment.");
            }

        } else {
            showError("Please select a fine to process.");
        }

    }

    @FXML
    private void waive() {
        Fine selectedFine = fine_table.getSelectionModel().getSelectedItem();
        if (selectedFine != null) {
            String loanId = selectedFine.getLoan_ID();
            String status = "Waived";
            String detailId = selectedFine.getDetail_ID();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setContentText("Are you sure you want to waive this fine? It mean the reader will not must pay this fine.");
            confirm.showAndWait();
            if (confirm.getResult().getText().equals("Cancel")) {
                return;
            }

            if (selectedFine.getPayment_Status().equals("Waived") || selectedFine.getPayment_Status().equals("Paid")) {
                showError("This fine has already been waived or paid.");
                return;
            }

            boolean updateSuccess = fineDAO.updatePaymentStatus(loanId, status, detailId);
            if (updateSuccess) {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setContentText("Fine waived successfully.");
                success.show();
                refreshFineTable();
            } else {
                showError("Failed to process payment.");
            }

        } else {
            showError("Please select a fine to process.");
        }

    }

    @FXML
    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(error);
        alert.show();
    }

}