package com.pdm.controller;

import java.time.LocalDate;
import java.util.List;

import com.pdm.dao.MemberDAO;
import com.pdm.dao.MemberDAO.MemberCategoryOption;
import com.pdm.model.Member;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class MemberManage extends basecontrol {
    @FXML
    private TableView<Member> mem_table;
    @FXML
    private TableColumn<Member, String> col_memid;
    @FXML
    private TableColumn<Member, String> col_name;
    @FXML
    private TableColumn<Member, String> col_email;
    @FXML
    private TableColumn<Member, String> col_cat;
    @FXML
    private TableColumn<Member, String> col_status;
    @FXML
    private TableColumn<Member, LocalDate> col_expiry;
    @FXML
    private TableColumn<Member, Integer> col_lim;
    @FXML
    private TableColumn<Member, Integer> col_period;

    @FXML
    private Label mem_count;
    @FXML
    private TextField search_field;
    @FXML
    private TextField id_field;
    @FXML
    private TextField name_field;
    @FXML
    private TextField email_field;
    @FXML
    private TextField identity_field;
    @FXML
    private DatePicker expiry_picker;
    @FXML
    private ChoiceBox<MemberCategoryOption> cat_select;
    @FXML
    private ChoiceBox<String> status_select;

    private final MemberDAO memberDAO = new MemberDAO();
    private final ObservableList<Member> memList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        col_memid.setCellValueFactory(new PropertyValueFactory<>("Member_ID"));
        col_name.setCellValueFactory(new PropertyValueFactory<>("Full_Name"));
        col_email.setCellValueFactory(new PropertyValueFactory<>("Email"));
        col_cat.setCellValueFactory(new PropertyValueFactory<>("Type"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("Account_Status"));
        col_expiry.setCellValueFactory(new PropertyValueFactory<>("Membership_Expiry_Date"));
        col_lim.setCellValueFactory(new PropertyValueFactory<>("Borrowing_Limit"));
        col_period.setCellValueFactory(new PropertyValueFactory<>("Loan_Period"));

        mem_table.setItems(memList);

        status_select.setItems(FXCollections.observableArrayList("Active", "Suspended", "Expired", "Locked"));
        status_select.setValue("Active");
        loadCategories();

        mem_table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });

        refreshMemTable();
        clearForm();
    }

    private void loadCategories() {
        List<MemberCategoryOption> categories = memberDAO.get_member_categories();
        cat_select.setItems(FXCollections.observableArrayList(categories));
        if (!categories.isEmpty()) {
            cat_select.setValue(categories.get(0));
        }
    }

    private void populateForm(Member member) {
        id_field.setText(member.getMember_ID());
        name_field.setText(member.getFull_Name());
        email_field.setText(member.getEmail());
        identity_field.setText(member.getIdentity_Card());
        expiry_picker.setValue(member.getMembership_Expiry_Date());
        status_select.setValue(member.getAccount_Status() != null ? member.getAccount_Status() : "Active");
        if (member.getType_ID() != null) {
            for (MemberCategoryOption option : cat_select.getItems()) {
                if (member.getType_ID().equals(option.getId())) {
                    cat_select.setValue(option);
                    break;
                }
            }
        }
    }

    @FXML
    private void clearForm() {
        mem_table.getSelectionModel().clearSelection();
        id_field.clear();
        name_field.clear();
        email_field.clear();
        identity_field.clear();
        expiry_picker.setValue(LocalDate.now().plusYears(1));
        status_select.setValue("Active");
        if (!cat_select.getItems().isEmpty() && cat_select.getValue() == null) {
            cat_select.setValue(cat_select.getItems().get(0));
        }
    }

    @FXML
    private void addMember() {
        MemberForm form = readForm(true);
        if (form == null) {
            return;
        }
        boolean success = memberDAO.addMember(form.typeId, form.fullName, form.email, form.identityCard, form.expiryDate,
                form.status);
        if (success) {
            refreshMemTable();
            clearForm();
            showInfo("Member added successfully.");
        } else {
            showError("Failed to add member. Check that Email/Identity Card are unique and valid.");
        }
    }

    @FXML
    private void updateMember() {
        String memberId = id_field.getText();
        if (memberId == null || memberId.isBlank()) {
            showError("Please select a member to update.");
            return;
        }
        MemberForm form = readForm(false);
        if (form == null) {
            return;
        }
        boolean success = memberDAO.updateMember(memberId, form.typeId, form.fullName, form.email, form.identityCard,
                form.expiryDate, form.status);
        if (success) {
            refreshMemTable();
            reselect(memberId);
            showInfo("Member updated successfully.");
        } else {
            showError("Failed to update member. Check that Email/Identity Card are unique and valid.");
        }
    }

    @FXML
    private void deleteMember() {
        Member selected = mem_table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a member to delete.");
            return;
        }
        boolean success = memberDAO.deleteMember(selected.getMember_ID());
        if (success) {
            refreshMemTable();
            clearForm();
            showInfo("Member deleted successfully.");
        } else {
            showError("Failed to delete member. Members with related records (loans, reservations, phones, etc.) cannot be deleted.");
        }
    }

    @Override
    @FXML
    public void search() {
        String text = search_field.getText();
        if (text == null || text.trim().isEmpty()) {
            refreshMemTable();
        } else {
            memList.setAll(memberDAO.search_members(text.trim()));
            mem_count.setText(memList.size() + " Members");
        }
    }

    private void refreshMemTable() {
        memList.setAll(memberDAO.get_member());
        mem_count.setText(memList.size() + " Members");
    }

    private void reselect(String memberId) {
        for (Member member : memList) {
            if (memberId.equals(member.getMember_ID())) {
                mem_table.getSelectionModel().select(member);
                populateForm(member);
                break;
            }
        }
    }

    private MemberForm readForm(boolean forAdd) {
        String fullName = name_field.getText() == null ? "" : name_field.getText().trim();
        String email = email_field.getText() == null ? "" : email_field.getText().trim();
        String identity = identity_field.getText() == null ? "" : identity_field.getText().trim();
        MemberCategoryOption category = cat_select.getValue();
        String status = status_select.getValue();
        LocalDate expiry = expiry_picker.getValue();

        if (fullName.isEmpty()) {
            showError("Full name is required.");
            return null;
        }
        if (category == null) {
            showError("Please select a membership category.");
            return null;
        }
        if (email.isEmpty() || !email.matches(".*\\S@\\S+\\.\\S+.*")) {
            showError("Please enter a valid email address.");
            return null;
        }
        if (identity.length() < 9 || identity.length() > 20) {
            showError("Identity card must be between 9 and 20 characters.");
            return null;
        }
        if (expiry == null) {
            showError("Please select a membership expiry date.");
            return null;
        }
        if (status == null || status.isBlank()) {
            showError("Please select an account status.");
            return null;
        }

        return new MemberForm(category.getId(), fullName, email, identity, expiry, status);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Members");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Members");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class MemberForm {
        final String typeId;
        final String fullName;
        final String email;
        final String identityCard;
        final LocalDate expiryDate;
        final String status;

        MemberForm(String typeId, String fullName, String email, String identityCard, LocalDate expiryDate, String status) {
            this.typeId = typeId;
            this.fullName = fullName;
            this.email = email;
            this.identityCard = identityCard;
            this.expiryDate = expiryDate;
            this.status = status;
        }
    }
}
