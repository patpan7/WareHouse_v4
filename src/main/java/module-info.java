module com.example.warehouse {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires org.controlsfx.controls;
    requires itextpdf;


    opens com.example.warehouse to javafx.fxml;
    exports com.example.warehouse;
}