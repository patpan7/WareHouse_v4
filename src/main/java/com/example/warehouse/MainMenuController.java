package com.example.warehouse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    public void mainMenuClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("main-menu.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void productClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("products.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void suppliersClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("suppliers.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void ordersClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("orders.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void buysClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("buys.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void departmentsClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("departments.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void intrashipmentClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("intrashipment.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void statisticsClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("statistics.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void settingsClick(ActionEvent e) throws IOException {
        root = FXMLLoader.load(getClass().getResource("settings.fxml"));
        stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}