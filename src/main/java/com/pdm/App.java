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
        // Points to the new view folder structure
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("view/HomeView.fxml"));
        Parent root = fxmlLoader.load();
        
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Library Management");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}