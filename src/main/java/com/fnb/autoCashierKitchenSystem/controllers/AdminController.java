package com.fnb.autoCashierKitchenSystem.controllers;

import com.fnb.autoCashierKitchenSystem.model.Order;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private HBox previewHeader;
    @FXML private ImageView previewLogo;
    @FXML private Label previewName;
    @FXML private TilePane menuGrid;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ColorPicker colorPicker;

    @FXML private Button addItemBtn;
    @FXML private Button removeItemBtn;
    @FXML private Button setNameBtn;
    @FXML private Button setIconBtn;
    @FXML private Button passwordBtn;
    @FXML private Label currentPasswordLabel;

    private KitchenController kitchenController;
    private List<Order> menuList;
    private String currentThemeHex = "#4338CA";

    public void setKitchenController(KitchenController controller) {
        this.kitchenController = controller;
        this.menuList = controller.fullMenu;
        refreshUI();
    }

    @FXML
    public void initialize() {
        if(addItemBtn != null) addItemBtn.setOnAction(e -> handleAdd());
        if(removeItemBtn != null) removeItemBtn.setOnAction(e -> handleRemove());
        if(setNameBtn != null) setNameBtn.setOnAction(e -> handleSetName());
        if(setIconBtn != null) setIconBtn.setOnAction(e -> handleSetIcon());
        if(colorPicker != null) colorPicker.setOnAction(e -> handleSetColor());
        if(passwordBtn != null) passwordBtn.setOnAction(e -> handlePasswordChange());

        categoryCombo.getItems().addAll("All Items", "Foods", "Beverages");
        categoryCombo.getSelectionModel().selectFirst();
        categoryCombo.setOnAction(e -> renderMenu());
    }

    private void refreshUI() {
        File dataDir = new File(KitchenController.DATA_DIR);
        File icon = new File(dataDir, KitchenController.ICON_FILE);
        if(icon.exists()) {
            try (InputStream is = new FileInputStream(icon)) { previewLogo.setImage(new Image(is)); } catch(Exception e){}
        }
        File name = new File(dataDir, KitchenController.NAME_FILE);
        if(name.exists()) {
            try { previewName.setText(Files.readString(name.toPath())); } catch(Exception e){}
        }
        File colorFile = new File(dataDir, KitchenController.COLOR_FILE);
        if(colorFile.exists()) {
            try {
                String hex = Files.readString(colorFile.toPath());
                applyTheme(hex);
                colorPicker.setValue(Color.web(hex));
            } catch(Exception e){}
        } else {
            applyTheme("#4338CA");
            colorPicker.setValue(Color.web("#4338CA"));
        }
        File passFile = new File(dataDir, KitchenController.PASS_FILE);
        if (passFile.exists()) {
            try { if (currentPasswordLabel != null) currentPasswordLabel.setText(Files.readString(passFile.toPath())); } catch(Exception e) {}
        }
        renderMenu();
    }

    private void handlePasswordChange() {
        TextInputDialog dialog = new TextInputDialog("");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/fnb/autoCashierKitchenSystem/utensil.png")));

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(icon);
        dialog.setTitle("Dineamic: Change Cashier Password");
        dialog.setHeaderText("Set password for Cashier Login");
        dialog.setContentText("New Password:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newPass -> {
            if (newPass.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Password cannot be empty!");
                alert.show();
                return;
            }
            try {
                File passFile = new File(KitchenController.DATA_DIR, KitchenController.PASS_FILE);
                Files.writeString(passFile.toPath(), newPass);
                if (currentPasswordLabel != null) currentPasswordLabel.setText(newPass);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cashier Password Updated!");
                alert.show();
            } catch (IOException e) { e.printStackTrace(); }
        });
    }

    private void applyTheme(String hex) {
        this.currentThemeHex = hex;
        if(previewHeader != null) previewHeader.setStyle("-fx-background-color: " + hex + ";");
        renderMenu();
    }

    private void handleSetColor() {
        Color c = colorPicker.getValue();
        String hex = String.format("#%02X%02X%02X", (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
        try {
            File colorFile = new File(KitchenController.DATA_DIR, KitchenController.COLOR_FILE);
            Files.writeString(colorFile.toPath(), hex);
            applyTheme(hex);
            if(kitchenController != null) kitchenController.triggerUpdate("COLOR");
        } catch(IOException e) { e.printStackTrace(); }
    }

    private void handleAdd() {
        try {
            URL url = getClass().getResource("/com/fnb/autoCashierKitchenSystem/addMenu.fxml");
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/fnb/autoCashierKitchenSystem/utensil.png")));

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AddNewMenuController controller = loader.getController();
            controller.setKitchenController(this.kitchenController);
            controller.setThemeColor(currentThemeHex);

            Stage stage = new Stage();
            stage.setTitle("Dineamic: Add New Menu Item");
            stage.getIcons().add(icon);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if(kitchenController != null) this.menuList = kitchenController.fullMenu;
            renderMenu();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleRemove() {
        if (menuList == null || menuList.isEmpty()) return;
        List<String> choices = new ArrayList<>();
        for (Order order : menuList) choices.add(order.getName());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Remove Item");
        dialog.setContentText("Select Item:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            menuList.removeIf(i -> i.getName().equals(name));
            try {
                File f = new File(KitchenController.DATA_DIR, KitchenController.MENU_FILE);
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) { out.writeObject(menuList); }
                if(kitchenController != null) kitchenController.triggerUpdate("MENU");
                renderMenu();
            } catch(IOException e){}
        });
    }

    private void handleSetName() {
        TextInputDialog dialog = new TextInputDialog(previewName.getText());
        dialog.setTitle("Set Name");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                File f = new File(KitchenController.DATA_DIR, KitchenController.NAME_FILE);
                Files.writeString(f.toPath(), name);
                refreshUI();
                if(kitchenController != null) kitchenController.triggerUpdate("NAME");
            } catch(IOException e){}
        });
    }

    private void handleSetIcon() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File file = fileChooser.showOpenDialog(addItemBtn.getScene().getWindow());
        if(file != null) {
            try {
                File dest = new File(KitchenController.DATA_DIR, KitchenController.ICON_FILE);
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                refreshUI();
                if(kitchenController != null) kitchenController.triggerUpdate("ICON");
            } catch(IOException e){}
        }
    }

    private void renderMenu() {
        menuGrid.getChildren().clear();
        String cat = categoryCombo.getValue();
        if (menuList == null) return;
        List<Order> display = (cat == null || cat.equals("All Items")) ? menuList :
                menuList.stream().filter(i -> i.getCategory().equalsIgnoreCase(cat)).collect(Collectors.toList());
        for (Order item : display) menuGrid.getChildren().add(createMenuButton(item));
    }

    private Button createMenuButton(Order item) {
        Button btn = new Button();
        btn.setPrefSize(150, 180);
        String defaultStyle = "-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);";
        btn.setStyle(defaultStyle);

        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 12; " +
                    "-fx-border-color: " + currentThemeHex + "; -fx-border-radius: 12; " +
                    "-fx-translate-y: -3; " +
                    "-fx-effect: dropshadow(three-pass-box, " + currentThemeHex + ", 15, 0, 0, 5);");
        });
        btn.setOnMouseExited(e -> btn.setStyle(defaultStyle + "-fx-translate-y: 0;"));

        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        VBox layout = new VBox(6);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(12, 5, 5, 5));

        ImageView imageView = new ImageView();
        imageView.setFitWidth(80); imageView.setFitHeight(80);

        String fileName = item.getImagePath();
        if (fileName != null && !fileName.equals("DEFAULT")) {
            File imgFile = new File(KitchenController.DATA_DIR, fileName);
            if (imgFile.exists()) {
                imageView.setImage(new Image(imgFile.toURI().toString()));
            }
        } else {
            try {
                imageView.setImage(new Image(getClass().getResource("/com/fnb/autoCashierKitchenSystem/utensil.png").toExternalForm()));
            } catch (Exception e) {}
        }

        centerImage(imageView, 80, 80);
        Rectangle clip = new Rectangle(80, 80);
        clip.setArcWidth(12); clip.setArcHeight(12);
        imageView.setClip(clip);

        VBox textInfo = new VBox(2);
        textInfo.setAlignment(Pos.CENTER);
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
        nameLabel.setWrapText(true); nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(130); nameLabel.setMinHeight(40); nameLabel.setAlignment(Pos.CENTER);

        Label priceLabel = new Label(String.format("$%.2f", item.getPrice()));
        priceLabel.setStyle("-fx-text-fill: " + currentThemeHex + "; -fx-font-weight: bold;");

        textInfo.getChildren().addAll(nameLabel, priceLabel);
        layout.getChildren().addAll(imageView, textInfo);
        btn.setGraphic(layout);
        return btn;
    }

    private void centerImage(ImageView imageView, double targetWidth, double targetHeight) {
        if (imageView.getImage() == null) return;
        double originalWidth = imageView.getImage().getWidth();
        double originalHeight = imageView.getImage().getHeight();
        double scale = Math.max(targetWidth / originalWidth, targetHeight / originalHeight);
        double cropWidth = targetWidth / scale;
        double cropHeight = targetHeight / scale;
        double x = (originalWidth - cropWidth) / 2;
        double y = (originalHeight - cropHeight) / 2;
        imageView.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
    }
}