package com.pdm.controller;

import java.sql.Date;

import com.pdm.dao.ReservationDAO;
import com.pdm.model.Reservation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private TableColumn<Reservation, Date> col_date;

    @FXML
    private Button cancel_btn;
    @FXML
    private Button fulfill_btn;

    private ObservableList<Reservation> reserList = FXCollections.observableArrayList();
    private ReservationDAO reservationDAO = new ReservationDAO();

    @FXML // after select a reservation it enable
    public void initialize() {
        fulfill_btn.disableProperty().bind(
                reser_table.getSelectionModel().selectedItemProperty().isNull());

        cancel_btn.disableProperty().bind(
                reser_table.getSelectionModel().selectedItemProperty().isNull());

        col_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        col_title.setCellValueFactory(new PropertyValueFactory<>("title"));
        col_memid.setCellValueFactory(new PropertyValueFactory<>("memid"));
        col_memname.setCellValueFactory(new PropertyValueFactory<>("memname"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("status"));
        col_date.setCellValueFactory(new PropertyValueFactory<>("date"));

        reser_table.setItems(reserList);
        refreshReserTable();

    }

    private void refreshReserTable() {

        reserList.setAll(reservationDAO.getReser());
    }

    @FXML
    private void cancel() {
        Reservation selectedReservation = reser_table.getSelectionModel().getSelectedItem();

        if (selectedReservation == null) {
            showError("Please select a reservation to cancel.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Are you sure you want to cancel this reservation?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
            } else {
                return;
            }
        });

        if (!"Pending".equals(selectedReservation.getStatus())) {
            showError("Only pending reservations can be canceled.");
            return;
        }

        boolean success = reservationDAO.cancelReser(selectedReservation.getId());

        if (success) {
            showError("Reservation canceled successfully.");
        } else {
            showError("Failed to cancel reservation.");
        }

        refreshReserTable();

    }

    private void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(error);
        alert.show();
    }

}