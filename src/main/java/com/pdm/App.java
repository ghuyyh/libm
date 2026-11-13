package com.pdm;

import java.io.IOException;
import com.pdm.connect.dbconnect;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        if (!dbconnect.testConnection()) {
    Alert alert = new Alert(Alert.AlertType.ERROR,
        "Could not connect to the database.\nCheck db.properties and make sure MySQL is running.");
    alert.setHeaderText("Database connection failed");
    alert.showAndWait();
    Platform.exit();
    return;
}
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