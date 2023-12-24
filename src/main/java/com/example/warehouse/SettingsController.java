package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController extends MainMenuController implements Initializable {

    @FXML
    TextField tfServer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Φόρτωση παραμέτρου
        String server = AppSettings.loadSetting("server");
        tfServer.setText(server);
    }

    public void saveSettings(ActionEvent event) throws IOException {
        AppSettings.saveSetting("server", tfServer.getText());
        mainMenuClick(event);
    }
}
