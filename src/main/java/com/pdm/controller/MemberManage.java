package com.pdm.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MemberManage {

    @FXML
    private BorderPane mainpane;

    @FXML
    public void homeClick(ActionEvent event) {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/pdm/view/HomeView.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) mainpane.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle("Library Management");
            stage.setScene(scene);
            stage.show();
            System.out.println("home ok");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
