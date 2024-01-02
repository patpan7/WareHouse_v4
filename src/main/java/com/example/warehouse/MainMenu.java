package com.example.warehouse;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenu extends Application {
    @Override
    public void start(Stage stage) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource("main-menu.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        stage.setTitle("Warehouse");
//        //stage.setResizable(false);
//        stage.setScene(scene);
//        stage.show();
            FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource("main-menu.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            MainMenuController controller = fxmlLoader.getController();
            controller.setStage(stage);
            controller.setScene(scene);
            controller.setRoot(fxmlLoader.getRoot());

            stage.setTitle("Warehouse");
            stage.setScene(scene);
            stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}