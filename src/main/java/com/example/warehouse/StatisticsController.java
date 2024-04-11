package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class StatisticsController {
    @FXML
    StackPane stackPane;
    @FXML
    private StackPane contentPane;

    public void itemStatisticsClick(ActionEvent event) throws  IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("itemsStatistics.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
    }

    public void suppliersStatisticsClick(ActionEvent event) throws  IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("suppliersStatistics.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
    }

    public void departmentStatisticsClick(ActionEvent event) throws  IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("departmentsStatistics.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
