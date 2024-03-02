package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ReturnsController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;
    @FXML
    TableView<Buys> supReturnsTable;
    @FXML
    TableView <Distribution> depReturnsTable;

    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
        // Πρώτη μέρα του τρέχοντος μήνα
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);
        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
