package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class EditOrderController extends MainMenuController implements Initializable {

    String date;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {


    }

    public void setData(Order selectedItem) {
        date = selectedItem.getDate();
    }

    public void saveAction(ActionEvent actionEvent) {

    }
}
