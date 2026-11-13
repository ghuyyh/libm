package com.pdm.controller;

import com.pdm.model.Reservation;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

public class ReservationManage extends basecontrol {
    @FXML
    private TableView<Reservation> reser_table;

    @FXML
    private Button cancel_btn;
    @FXML
    private Button fulfill_btn;

    @FXML // after select a reservation it enable
    public void initialize() {
        fulfill_btn.disableProperty().bind(
                reser_table.getSelectionModel().selectedItemProperty().isNull());

        cancel_btn.disableProperty().bind(
                reser_table.getSelectionModel().selectedItemProperty().isNull());
    }

}