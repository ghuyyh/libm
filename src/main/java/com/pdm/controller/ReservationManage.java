package com.pdm.controller;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.pdm.dao.ReservationDAO;
import com.pdm.model.Reservation;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ReservationManage extends basecontrol {
    @FXML
    private TableView<Reservation> reser_table;
    @FXML
    private TableColumn<Reservation, String> col_id;
    @FXML
    private TableColumn<Reservation, String> col_title;
    @FXML
    private TableColumn<Reservation, String> col_memid;
    @FXML
    private TableColumn<Reservation, String> col_memname;
    @FXML
    private TableColumn<Reservation, String> col_status;
    @FXML
    private TableColumn<Reservation, java.sql.Date> col_date;
    @FXML
    private TableColumn<Reservation, Integer> col_available;

    @FXML
    private Button cancel_btn;
    @FXML
    private Button fulfill_btn;

    // Add tab
    @FXML
    private TextField new_memid;
    @FXML
    private TextField new_isbn;
    @FXML
    private DatePicker new_date;

    // Fulfill tab
    @FXML
    private ChoiceBox<String> copy_select;
    @FXML
    private TextField fulfill_staff;

    @FXML
    private TextField search_field;

    private final ObservableList<Reservation> reserList = FXCollections.observableArrayList();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    @FXML
    public void initialize() {
        new_date.setValue(LocalDate.now());

        cancel_btn.disableProperty().bind(
                reser_table.getSelectionModel().selectedItemProperty().isNull());

        col_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        col_title.setCellValueFactory(new PropertyValueFactory<>("title"));
        col_memid.setCellValueFactory(new PropertyValueFactory<>("memid"));
        col_memname.setCellValueFactory(new PropertyValueFactory<>("memname"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("status"));
        col_date.setCellValueFactory(new PropertyValueFactory<>("date"));
        col_available.setCellValueFactory(cell -> new SimpleObjectProperty<>(
                cell.getValue().getAvailableCopies()));

        reser_table.setItems(reserList);
        reser_table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, selected) -> loadAvailableCopies(selected));
        copy_select.valueProperty().addListener((obs, oldValue, newValue) -> updateFulfillButton());

        refreshReserTable();
    }

    // ------------------------------------------------------------------
    // Table loading / filtering
    // ------------------------------------------------------------------
    private void refreshReserTable() {
        try {
            reserList.setAll(reservationDAO.getReser());
        } catch (SQLException e) {
            showError("Cannot load reservations", e.getMessage());
        }
    }

    @Override
    @FXML
    public void search() {
        String keyword = text(search_field);
        try {
            List<Reservation> result = keyword.isEmpty()
                    ? reservationDAO.getReser()
                    : reservationDAO.searchReser(keyword);
            reserList.setAll(result);
        } catch (SQLException e) {
            showError("Cannot search reservations", e.getMessage());
        }
    }

    private void loadAvailableCopies(Reservation selected) {
        copy_select.getItems().clear();
        copy_select.setValue(null);
        if (selected == null) {
            fulfill_btn.setDisable(true);
            return;
        }
        try {
            copy_select.getItems().setAll(reservationDAO.getAvailableCopies(selected.getBookid()));
            if (!copy_select.getItems().isEmpty()) {
                copy_select.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showError("Cannot load available copies", e.getMessage());
        }
        updateFulfillButton();
    }

    private void updateFulfillButton() {
        Reservation selected = reser_table.getSelectionModel().getSelectedItem();
        fulfill_btn.setDisable(selected == null
                || copy_select.getValue() == null
                || !"Pending".equals(selected.getStatus()));
    }

    // ------------------------------------------------------------------
    // Add new reservation (RS2 INSERT)
    // ------------------------------------------------------------------
    @FXML
    public void add() {
        String memberId = text(new_memid);
        String isbn = text(new_isbn);
        if (memberId.isEmpty() || isbn.isEmpty()) {
            showError("Missing information", "Please enter a Member ID and an ISBN.");
            return;
        }

        LocalDate date = new_date.getValue() == null ? LocalDate.now() : new_date.getValue();
        try {
            String bookId = reservationDAO.findBookIdByIsbn(isbn);
            if (bookId == null) {
                showError("Book not found", "No book with ISBN " + isbn + ".");
                return;
            }
            String reservationId = reservationDAO.addReser(memberId, bookId, date);
            new_memid.clear();
            new_isbn.clear();
            new_date.setValue(LocalDate.now());
            refreshReserTable();
            showInfo("Reservation " + reservationId + " created.");
        } catch (SQLException e) {
            showError("Cannot add reservation", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Cancel pending reservation (RS2 status update)
    // ------------------------------------------------------------------
    @FXML
    public void cancel() {
        Reservation selected = reser_table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Please select a reservation to cancel.");
            return;
        }
        if (!"Pending".equals(selected.getStatus())) {
            showError("Cannot cancel", "Only pending reservations can be canceled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to cancel this reservation?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            if (!reservationDAO.cancelReser(selected.getId())) {
                showError("Cannot cancel", "This reservation is no longer pending.");
                return;
            }
            refreshReserTable();
            showInfo("Reservation " + selected.getId() + " canceled.");
        } catch (SQLException e) {
            showError("Cannot cancel reservation", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Fulfill reservation: create loan, mark copy borrowed, close reservation
    // ------------------------------------------------------------------
    @FXML
    public void fulfill() {
        Reservation selected = reser_table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Please select a reservation to fulfill.");
            return;
        }
        if (!"Pending".equals(selected.getStatus())) {
            showError("Cannot fulfill", "Only pending reservations can be fulfilled.");
            return;
        }
        String copyId = copy_select.getValue();
        if (copyId == null) {
            showError("No copy selected", "Select an available copy first.");
            return;
        }
        String staffId = text(fulfill_staff);
        if (staffId.isEmpty()) {
            showError("Missing information", "Please enter the Staff ID handling this loan.");
            return;
        }

        try {
            String loanId = reservationDAO.fulfillReser(
                    selected.getId(), selected.getMemid(), selected.getBookid(), copyId, staffId);
            refreshReserTable();
            showInfo("Reservation fulfilled. Loan " + loanId + " created (copy " + copyId + ").");
        } catch (SQLException e) {
            showError("Cannot fulfill reservation", e.getMessage());
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Reservation Management");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reservation Management");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
