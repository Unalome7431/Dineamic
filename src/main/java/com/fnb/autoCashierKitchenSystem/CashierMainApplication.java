package com.fnb.autoCashierKitchenSystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class CashierMainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/fnb/autoCashierKitchenSystem/utensil.png")));

        Parent root = loader.load();

        Scene scene = new Scene(root);

        primaryStage.setTitle("Dineamic: Cashier Station - Login Network");
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}