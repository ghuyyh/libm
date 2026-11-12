package com.pdm.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class FinePaymentManage extends basecontrol {

@FXML
    ChoiceBox<String> payment_select = new ChoiceBox<>();

    @FXML
    public void initialize() {
        payment_select.getItems().addAll("Cash", "Bank transfer", "Card");
        payment_select.setValue("Cash");// de day sua sau
    }


}