module com.example.warehouse {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.example.warehouse to javafx.fxml;
    exports com.example.warehouse;
}