package com.pdm;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        //get home view
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("view/HomeView.fxml"));
        Parent root = fxmlLoader.load();
        
        Scene scene = new Scene(root);
        stage.setTitle("Library Management");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}