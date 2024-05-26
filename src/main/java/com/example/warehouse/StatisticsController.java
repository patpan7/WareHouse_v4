package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class StatisticsController {
    @FXML
    StackPane stackPane;
    @FXML
    private StackPane contentPane;
    @FXML
    Button btnItems;
    @FXML
    Button btnItems2;
    @FXML
    Button btnSuppliers;
    @FXML
    Button btnSuppliers2;
    @FXML
    Button btnDepartments;

    public void itemStatisticsClick(ActionEvent event) throws  IOException {
        //Στατιστικά ειδών αναλυτικά
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("itemsStatistics.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
        btnItems.setDefaultButton(true);
        btnItems2.setDefaultButton(false);
        btnSuppliers.setDefaultButton(false);
        btnSuppliers2.setDefaultButton(false);
        btnDepartments.setDefaultButton(false);
    }
    public void itemStatistics2Click(ActionEvent event) throws  IOException {
        //Στατιστικά ειδών συνοπτικά
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("itemsStatistics2.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
        btnItems.setDefaultButton(false);
        btnItems2.setDefaultButton(true);
        btnSuppliers.setDefaultButton(false);
        btnSuppliers2.setDefaultButton(false);
        btnDepartments.setDefaultButton(false);
    }

    public void suppliersStatisticsClick(ActionEvent event) throws  IOException {
        //Στατιστικά προμηθευτών αναλυτικά
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("suppliersStatistics2.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
        btnItems.setDefaultButton(false);
        btnItems2.setDefaultButton(false);
        btnSuppliers.setDefaultButton(true);
        btnSuppliers2.setDefaultButton(false);
        btnDepartments.setDefaultButton(false);
    }

    public void suppliersStatistics2Click(ActionEvent event) throws  IOException {
        //Στατιστικά προμηθευτών συνοπτικά
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("suppliersStatistics2.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
        btnItems.setDefaultButton(false);
        btnItems2.setDefaultButton(false);
        btnSuppliers.setDefaultButton(false);
        btnSuppliers2.setDefaultButton(true);
        btnDepartments.setDefaultButton(false);
    }

    public void departmentStatisticsClick(ActionEvent event) throws  IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("departmentsStatistics.fxml"));
        Parent  root = fxmlLoader.load();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(root);
        btnItems.setDefaultButton(false);
        btnItems2.setDefaultButton(false);
        btnSuppliers.setDefaultButton(false);
        btnSuppliers2.setDefaultButton(false);
        btnDepartments.setDefaultButton(true);
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
