package com.pdm.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class MemberManage extends basecontrol {
    @FXML
    ChoiceBox<String> cat_select = new ChoiceBox<>();

    @FXML
    public void initialize() {
        cat_select.getItems().addAll("Student", "Staff", "External");
        cat_select.setValue("Student");// de day sua sau
    }

}
