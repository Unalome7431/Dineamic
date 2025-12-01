package com.fnb.autoCashierKitchenSystem;

import com.fnb.autoCashierKitchenSystem.controllers.KitchenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class KitchenMainApplication extends Application {

    private KitchenController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("kitchen.fxml"));
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/fnb/autoCashierKitchenSystem/utensil.png")));

            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("Dineamic: Kitchen Display");
            primaryStage.getIcons().add(icon);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
