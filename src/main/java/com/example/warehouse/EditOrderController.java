package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class EditOrderController extends MainMenuController implements Initializable {

    @FXML
    DatePicker orderDate;
    String date;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println(date);
        orderDate.setValue(LocalDate.now());
    }

    public void setData(Order selectedItem) {
        date = selectedItem.getDate();
    }

    public void saveAction(ActionEvent actionEvent) {

    }
}
