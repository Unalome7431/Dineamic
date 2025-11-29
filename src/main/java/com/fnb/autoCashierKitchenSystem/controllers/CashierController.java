package com.fnb.autoCashierKitchenSystem.controllers;

import com.fnb.autoCashierKitchenSystem.model.CartItem;
import com.fnb.autoCashierKitchenSystem.model.Order;
import com.fnb.autoCashierKitchenSystem.model.OrderPayload;
import com.fnb.autoCashierKitchenSystem.model.TicketCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CashierController {

    @FXML private TilePane menuGrid;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Spinner<Integer> tableSpinner;
    @FXML private ListView<CartItem> ticketList;
    @FXML private Label totalLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Button payButton;
    @FXML private ImageView brandLogo;
    @FXML private Label brandNameLabel;
    @FXML private HBox headerBar;

    public List<Order> fullMenu = new ArrayList<>();
    public static ObservableList<CartItem> currentOrder = FXCollections.observableArrayList();

    private static String KITCHEN_IP;
    private static final int KITCHEN_PORT = 12345;

    // Local cache for downloaded images
    private static final File LOCAL_CACHE_DIR = new File(System.getProperty("user.home"), "CashierApp_Cache");

    private long currentIconTimestamp = 0;
    private long currentNameTimestamp = 0;
    private long currentMenuTimestamp = 0;
    private long currentColorTimestamp = 0;

    private String currentThemeHex = "#4338CA";
    private ScheduledExecutorService syncExecutor;

    public void initIPAddress(String ipAddress) {
        KITCHEN_IP = ipAddress;
        startSyncService();
    }

    @FXML
    public void initialize() {
        // Ensure cache directory exists
        if (!LOCAL_CACHE_DIR.exists()) LOCAL_CACHE_DIR.mkdirs();

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        tableSpinner.setValueFactory(valueFactory);

        categoryCombo.getItems().addAll("All Items", "Foods", "Beverages");
        categoryCombo.getSelectionModel().selectFirst();
        categoryCombo.setOnAction(e -> filterMenu());

        renderMenu(fullMenu);

        payButton.setOnAction(e -> sendOrderToKitchen());

        ticketList.setItems(currentOrder);
        currentOrder.addListener((ListChangeListener<CartItem>) c -> calculateTotal());
        ticketList.setCellFactory(param -> new TicketCell());
    }

    private void startSyncService() {
        if (syncExecutor != null && !syncExecutor.isShutdown()) return;
        syncExecutor = Executors.newSingleThreadScheduledExecutor();
        syncExecutor.scheduleAtFixedRate(this::checkAndFetchUpdates, 0, 1, TimeUnit.SECONDS);
    }

    private void checkAndFetchUpdates() {
        if (KITCHEN_IP == null || KITCHEN_IP.isEmpty()) return;

        try (Socket socket = new Socket(KITCHEN_IP, KITCHEN_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("GET_MENU_TIMESTAMP"); out.flush();
            long menuTime = in.readLong();
            if (menuTime > currentMenuTimestamp) fetchMenu(out, in, menuTime);

            out.writeObject("GET_ICON_TIMESTAMP"); out.flush();
            long iconTime = in.readLong();
            if (iconTime > currentIconTimestamp) fetchIconImage(out, in, iconTime);

            out.writeObject("GET_NAME_TIMESTAMP"); out.flush();
            long nameTime = in.readLong();
            if (nameTime > currentNameTimestamp) fetchName(out, in, nameTime);

            out.writeObject("GET_COLOR_TIMESTAMP"); out.flush();
            long colorTime = in.readLong();
            if (colorTime > currentColorTimestamp) fetchColor(out, in, colorTime);

        } catch (Exception e) {}
    }

    private void fetchMenu(ObjectOutputStream out, ObjectInputStream in, long serverTime) throws IOException, ClassNotFoundException {
        out.writeObject("GET_MENU"); out.flush();
        List<Order> newMenu = (List<Order>) in.readObject();
        if (newMenu != null) {
            fullMenu = newMenu;
            currentMenuTimestamp = serverTime;
            Platform.runLater(this::filterMenu);
        }
    }

    private void fetchIconImage(ObjectOutputStream out, ObjectInputStream in, long serverTime) throws IOException, ClassNotFoundException {
        out.writeObject("GET_ICON"); out.flush();
        byte[] imageBytes = (byte[]) in.readObject();
        if (imageBytes != null && imageBytes.length > 0) {
            Platform.runLater(() -> {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                    brandLogo.setImage(new Image(bis));
                    currentIconTimestamp = serverTime;
                } catch (Exception e) {}
            });
        }
    }

    private void fetchName(ObjectOutputStream out, ObjectInputStream in, long serverTime) throws IOException, ClassNotFoundException {
        out.writeObject("GET_NAME"); out.flush();
        String name = (String) in.readObject();
        if (name != null) {
            Platform.runLater(() -> {
                brandNameLabel.setText(name);
                currentNameTimestamp = serverTime;
            });
        }
    }

    private void fetchColor(ObjectOutputStream out, ObjectInputStream in, long serverTime) throws IOException, ClassNotFoundException {
        out.writeObject("GET_COLOR"); out.flush();
        String hex = (String) in.readObject();
        if (hex != null) {
            Platform.runLater(() -> {
                applyTheme(hex);
                currentColorTimestamp = serverTime;
            });
        }
    }

    private void applyTheme(String hex) {
        this.currentThemeHex = hex;
        if(headerBar != null) headerBar.setStyle("-fx-background-color: " + hex + ";");
        if(payButton != null) payButton.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 8;");
        if(totalLabel != null) totalLabel.setStyle("-fx-text-fill: " + hex + "; -fx-font-weight: bold; -fx-font-size: 24px;");
        filterMenu();
    }

    public void shutdown() { if (syncExecutor != null) syncExecutor.shutdownNow(); }

    private void filterMenu() {
        String selectedCategory = categoryCombo.getValue();
        if (selectedCategory == null || selectedCategory.equals("All Items")) {
            renderMenu(fullMenu);
        } else {
            List<Order> filtered = new ArrayList<>();
            for(Order item : fullMenu) {
                if(item.getCategory().equalsIgnoreCase(selectedCategory)) filtered.add(item);
            }
            renderMenu(filtered);
        }
    }

    private void renderMenu(List<Order> items) {
        menuGrid.getChildren().clear();
        for (Order item : items) {
            menuGrid.getChildren().add(createMenuButton(item));
        }
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

        // --- NEW IMAGE LOADING LOGIC --- TES
        String fileName = item.getImagePath();
        if (fileName != null && !fileName.equals("DEFAULT")) {
            File localFile = new File(LOCAL_CACHE_DIR, fileName);
            if (localFile.exists()) {
                // If we have it, show it
                imageView.setImage(new Image(localFile.toURI().toString()));
            } else {
                // If we don't, download it in background
                downloadImage(fileName, imageView);
            }
        } else {
            // Load Default
            try {
                imageView.setImage(new Image(getClass().getResource("/utensil-icon.jpg").toExternalForm()));
            } catch (Exception e) {}
        }
        // -------------------------------

        centerImage(imageView, 80, 80);
        Rectangle clip = new Rectangle(80, 80);
        clip.setArcWidth(12); clip.setArcHeight(12);
        imageView.setClip(clip);

        VBox textInfo = new VBox(2);
        textInfo.setAlignment(Pos.CENTER);
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill: #374151; -fx-font-weight: bold;");
        nameLabel.setFont(Font.font("System", 14));
        nameLabel.setWrapText(true); nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(130); nameLabel.setMinHeight(40); nameLabel.setAlignment(Pos.CENTER);

        Label priceLabel = new Label(String.format("$%.2f", item.getPrice()));
        priceLabel.getStyleClass().add("price-tag");
        priceLabel.setFont(Font.font(13));
        priceLabel.setStyle("-fx-text-fill: " + currentThemeHex + "; -fx-font-weight: bold;");

        textInfo.getChildren().addAll(nameLabel, priceLabel);
        layout.getChildren().addAll(imageView, textInfo);
        btn.setGraphic(layout);
        btn.setOnAction(e -> addToOrder(item));
        return btn;
    }

    private void downloadImage(String fileName, ImageView targetView) {
        new Thread(() -> {
            try (Socket socket = new Socket(KITCHEN_IP, KITCHEN_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject("GET_IMAGE_DATA:" + fileName);
                out.flush();

                byte[] data = (byte[]) in.readObject();
                if (data != null && data.length > 0) {
                    // Save to local cache
                    File target = new File(LOCAL_CACHE_DIR, fileName);
                    Files.write(target.toPath(), data);

                    // Update UI
                    Platform.runLater(() -> targetView.setImage(new Image(target.toURI().toString())));
                }
            } catch (Exception e) {
                // System.out.println("Could not fetch image: " + fileName);
            }
        }).start();
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

    private void addToOrder(Order item) {
        for (CartItem cartItem : currentOrder) {
            if (cartItem.getOrder().getName().equals(item.getName())) {
                cartItem.increment();
                ticketList.refresh();
                calculateTotal();
                return;
            }
        }
        currentOrder.add(new CartItem(item, 1));
    }

    private void calculateTotal() {
        double subtotal = 0.0;
        for (CartItem item : currentOrder) {
            subtotal += item.getTotalPrice();
        }
        double tax = subtotal * 0.10;
        double total = subtotal + tax;

        if(subtotalLabel != null) subtotalLabel.setText(String.format("$%.2f", subtotal));
        if(taxLabel != null) taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }

    private void sendOrderToKitchen() {
        if (currentOrder.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Order", "Please add items before ordering.");
            return;
        }
        int tableNum = tableSpinner.getValue();
        List<Order> flatList = new ArrayList<>();
        for (CartItem cartItem : currentOrder) {
            for (int i = 0; i < cartItem.getQuantity(); i++) {
                flatList.add(cartItem.getOrder());
            }
        }
        OrderPayload payload = new OrderPayload(tableNum, flatList);
        try (Socket socket = new Socket(KITCHEN_IP, KITCHEN_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(payload);
            out.flush();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Order sent to kitchen for Table " + tableNum);
            currentOrder.clear();
            calculateTotal();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to Kitchen (" + KITCHEN_IP + ":" + KITCHEN_PORT + ")");
        }
    }

    public void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}