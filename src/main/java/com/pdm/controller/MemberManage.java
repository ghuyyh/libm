package com.pdm.controller;

import com.pdm.dao.MemberDAO;
import com.pdm.model.Member;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MemberManage extends basecontrol {
    @FXML
    ChoiceBox<String> cat_select = new ChoiceBox<>();

    @FXML
    private TableView<Member> mem_table;
    @FXML
    private TableColumn<Member, String> col_memid;
    @FXML
    private TableColumn<Member, String> col_name;
    @FXML
    private TableColumn<Member, String> col_cat;
    @FXML
    private TableColumn<Member, Integer> col_lim;
    @FXML
    private TableColumn<Member, Integer> col_period;

        private MemberDAO memberDAO = new MemberDAO();
    private ObservableList<Member> memList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cat_select.getItems().addAll("Student", "Staff", "External");
        cat_select.setValue("Student");// de day sua sau

        col_memid.setCellValueFactory(new PropertyValueFactory<>("Member_ID"));
        col_name.setCellValueFactory(new PropertyValueFactory<>("Full_Name"));
        col_cat.setCellValueFactory(new PropertyValueFactory<>("Type"));
        col_lim.setCellValueFactory(new PropertyValueFactory<>("Borrowing_Limit"));
        col_period.setCellValueFactory(new PropertyValueFactory<>("Loan_Period"));

        mem_table.setItems(memList);

        refreshMemTable();

    }

    private void refreshMemTable() {
        memList.setAll(memberDAO.get_member());
    }

}
