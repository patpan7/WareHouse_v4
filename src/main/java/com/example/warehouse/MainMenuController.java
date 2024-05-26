package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private StackPane stackPane;
    @FXML
    Label vesrion;
    private Stage stage;
    private Scene scene;
    public Parent root;

    public void mainMenuClick(StackPane stackPane) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-menu.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void productClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("items.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("items.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void suppliersClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("suppliers.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("suppliers.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void ordersClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("orders.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("orders.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void buysClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("buys.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("buys.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void returnsClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("returns.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("returns.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void intrashipmentClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("distribution.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("distribution.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void statisticsClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("statistics.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("statistics.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void settingsClick(ActionEvent e) throws IOException {
//        root = FXMLLoader.load(getClass().getResource("settings.fxml"));
//        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        stage.setScene(scene);
//        stage.show();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("settings.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}