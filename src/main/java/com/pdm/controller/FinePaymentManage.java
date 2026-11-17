package com.pdm.controller;

import com.pdm.dao.FineDAO;
import com.pdm.model.Fine;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
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
    private ChoiceBox<String> payment_select = new ChoiceBox<>();

    private ObservableList<Fine> fine_list = FXCollections.observableArrayList();
    private FineDAO fineDAO = new FineDAO();

    @FXML
    public void initialize() {
        payment_select.getItems().addAll("Cash", "Bank transfer", "Card");
        payment_select.setValue("Cash");// de day sua sau

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
    }

    public void refreshFineTable() {
        fine_list.setAll(fineDAO.get_fines());
    }

}