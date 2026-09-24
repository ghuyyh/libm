module com.pdm {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;

    // Grants JavaFX access to your main application class
    opens com.pdm to javafx.fxml;
    exports com.pdm;

    // Grants JavaFX access to your UI controllers
    opens com.pdm.controller to javafx.fxml;
    exports com.pdm.controller;

    // Grants JavaFX access to your models (needed later for TableViews)
    opens com.pdm.model to javafx.base;
    exports com.pdm.model;
}
