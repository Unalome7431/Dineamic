package com.fnb.autoCashierKitchenSystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CashierMainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Ensure the FXML file is named 'ModernLayout.fxml' and is in the same package
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));

        // We need to manually set the controller factory if you want to pass dependencies, 
        // but for this simple setup, just ensure your FXML has: fx:controller="CashierController

        Parent root = loader.load();

        Scene scene = new Scene(root);

        primaryStage.setTitle("Cashier Station - Login Network");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}