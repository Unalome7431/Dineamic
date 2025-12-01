package com.fnb.autoCashierKitchenSystem.controllers;

import com.fnb.autoCashierKitchenSystem.model.Order;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AddNewMenuController {
    @FXML private Button setImageButton;
    @FXML private Button addButton;
    @FXML private ImageView imagePreview;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> categoryField;

    private File selectedImageFile;
    private KitchenController kitchenController;

    private static final String DEFAULT_MARKER = "DEFAULT";
    private static final String DEFAULT_ICON_PATH = "/com/fnb/autoCashierKitchenSystem/utensil.png";

    public void setKitchenController(KitchenController controller) {
        this.kitchenController = controller;
    }

    public void setThemeColor(String hex) {
        if (addButton != null && hex != null) {
            addButton.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void initialize() {
        try {
            if (getClass().getResource(DEFAULT_ICON_PATH) != null) {
                imagePreview.setImage(new Image(getClass().getResource(DEFAULT_ICON_PATH).toExternalForm()));
            }
        } catch (Exception e) {}

        setImageButton.setOnAction(e -> setImage());
        addButton.setOnAction(e -> addNewMenu());

        if (categoryField != null) {
            categoryField.getItems().clear();
            categoryField.getItems().addAll("Select", "Foods", "Beverages");
            categoryField.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void setImage() {
        Stage stage = (Stage) setImageButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Menu Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imagePreview.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    @FXML
    private void addNewMenu() {
        if (nameField.getText().isEmpty() || priceField.getText().isEmpty() ||
                categoryField.getValue() == null || categoryField.getValue().equals("Select")) {

            Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill out all fields.");
            alert.showAndWait();
            return;
        }

        try {
            String name = nameField.getText();
            double price = Double.parseDouble(priceField.getText());
            String category = categoryField.getValue();

            String imageIdentifier;

            if (selectedImageFile != null) {
                String savedName = saveImageExternal(selectedImageFile);
                imageIdentifier = (savedName != null) ? savedName : DEFAULT_MARKER;
            } else {
                imageIdentifier = DEFAULT_MARKER;
            }

            Order newOrder = new Order(name, price, category, imageIdentifier);

            if (kitchenController != null) {
                kitchenController.addMenuItem(newOrder);
            }

            Stage stage = (Stage) addButton.getScene().getWindow();
            stage.close();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Price must be a valid number.");
            alert.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String saveImageExternal(File sourceFile) {
        try {
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, "CashierApp_Images");
            if (!appDir.exists()) appDir.mkdirs();

            File destFile = getUniqueFile(appDir, sourceFile.getName());

            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return destFile.getName();
        } catch (IOException e) { return null; }
    }

    private File getUniqueFile(File directory, String originalName) {
        File file = new File(directory, originalName);
        if (!file.exists()) return file;

        String baseName = originalName;
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        int counter = 1;
        while (file.exists()) {
            String newName = baseName + "(" + counter + ")" + extension;
            file = new File(directory, newName);
            counter++;
        }
        return file;
    }
}