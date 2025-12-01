package com.fnb.autoCashierKitchenSystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.net.URL;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

public class NetworkController {
    @FXML private TextField ipInput;
    @FXML private PasswordField passwordInput;
    @FXML private Button connectButton;

    @FXML
    public void initialize() {
        connectButton.setOnAction(this::attemptLogin);
    }

    private void attemptLogin(ActionEvent event) {
        String ipAddress = ipInput.getText().trim();
        String password = passwordInput.getText().trim();

        if (ipAddress.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Info", "Please enter both IP and Password.");
            return;
        }

        connectButton.setText("Connecting...");
        connectButton.setDisable(true);

        new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ipAddress, 12345), 2000);

                try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    out.writeObject("LOGIN:" + password);
                    out.flush();

                    boolean loginSuccess = in.readBoolean();

                    javafx.application.Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        connectButton.setText("Connect & Login");

                        if (loginSuccess) {
                            switchToCashier(event, ipAddress);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Login Failed", "Incorrect Password.");
                        }
                    });
                }
            } catch (IOException e) {
                javafx.application.Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    connectButton.setText("Connect & Login");
                    showAlert(Alert.AlertType.ERROR, "Connection Failed", "Could not connect to Kitchen at " + ipAddress);
                });
            }
        }).start();
    }

    private void switchToCashier(ActionEvent event, String ipAddress) {
        try {
            URL url = getClass().getResource("/com/fnb/autoCashierKitchenSystem/cashier.fxml");
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/fnb/autoCashierKitchenSystem/utensil.png")));


            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            CashierController cashierController = loader.getController();
            cashierController.initIPAddress(ipAddress);

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setTitle("Dineamic: Cashier Station - Main");
            stage.getIcons().add(icon);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Critical Error",
                    "Failed to load Cashier Interface.\nError: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}