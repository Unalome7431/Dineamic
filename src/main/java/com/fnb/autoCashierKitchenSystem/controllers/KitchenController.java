package com.fnb.autoCashierKitchenSystem.controllers;

import com.fnb.autoCashierKitchenSystem.model.Order;
import com.fnb.autoCashierKitchenSystem.model.OrderPayload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class KitchenController {

    @FXML private TilePane ticketGrid;
    @FXML private Label totalOrdersLabel;
    @FXML private Button openAdminButton;
    @FXML private ImageView kitchenBrandLogo;
    @FXML private Label restaurantNameLabel;
    @FXML private Label serverIpLabel;
    @FXML private HBox headerBar;

    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private boolean isRunning = true;

    public static final String DATA_DIR = System.getProperty("user.home") + File.separator + "CashierApp_Images";

    public static final String ICON_FILE = "server_icon.png";
    public static final String NAME_FILE = "server_name.txt";
    public static final String MENU_FILE = "server_menu.ser";
    public static final String COLOR_FILE = "server_color.txt";
    public static final String PASS_FILE = "server_pass.txt";

    public List<Order> fullMenu = new ArrayList<>();

    private long iconLastModified = System.currentTimeMillis();
    private long nameLastModified = System.currentTimeMillis();
    private long menuLastModified = System.currentTimeMillis();
    private long colorLastModified = System.currentTimeMillis();

    private String currentThemeColor = "#4338CA";

    @FXML
    public void initialize() {
        openAdminButton.setOnAction(e -> openAdminDashboard());

        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();

        ensurePasswordFile();
        refreshLocalUI();
        displayServerIP();

        Thread serverThread = new Thread(this::startServer);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void ensurePasswordFile() {
        File passFile = new File(DATA_DIR, PASS_FILE);
        if (!passFile.exists()) {
            try {
                Files.writeString(passFile.toPath(), "admin");
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void displayServerIP() {
        new Thread(() -> {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                String ip = socket.getLocalAddress().getHostAddress();
                Platform.runLater(() -> serverIpLabel.setText(ip));
            } catch (Exception e) { Platform.runLater(() -> serverIpLabel.setText("Unknown IP")); }
        }).start();
    }

    public void triggerUpdate(String type) {
        long now = System.currentTimeMillis();
        switch (type) {
            case "MENU": menuLastModified = now; break;
            case "ICON": iconLastModified = now; break;
            case "NAME": nameLastModified = now; break;
            case "COLOR": colorLastModified = now; break;
        }
        refreshLocalUI();
    }

    public void refreshLocalUI() {
        loadLocalIcon();
        loadLocalName();
        loadLocalColor();
        loadMenuData();
    }

    private void openAdminDashboard() {
        try {
            URL url = getClass().getResource("/com/fnb/autoCashierKitchenSystem/admin.fxml");
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/fnb/autoCashierKitchenSystem/utensil.png")));

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            AdminController adminController = loader.getController();
            adminController.setKitchenController(this);
            Stage stage = new Stage();
            stage.setTitle("Dineamic: Admin Dashboard");
            stage.getIcons().add(icon);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) { e.printStackTrace(); }
    }

    public void addMenuItem(Order newItem) {
        fullMenu.add(newItem);
        try {
            File menuFile = new File(DATA_DIR, MENU_FILE);
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(menuFile))) {
                out.writeObject(fullMenu);
            }
            triggerUpdate("MENU");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadLocalIcon() {
        try {
            File iconFile = new File(DATA_DIR, ICON_FILE);
            if (iconFile.exists()) {
                try (InputStream is = new FileInputStream(iconFile)) {
                    kitchenBrandLogo.setImage(new Image(is));
                }
            }
        } catch (Exception e) {}
    }

    private void loadLocalName() {
        try {
            File nameFile = new File(DATA_DIR, NAME_FILE);
            if (nameFile.exists()) {
                String name = Files.readString(nameFile.toPath());
                Platform.runLater(() -> restaurantNameLabel.setText(name));
            }
        } catch (Exception e) {}
    }

    private void loadLocalColor() {
        try {
            File colorFile = new File(DATA_DIR, COLOR_FILE);
            if (colorFile.exists()) {
                String hex = Files.readString(colorFile.toPath());
                this.currentThemeColor = hex;
                Platform.runLater(() -> {
                    if (headerBar != null) headerBar.setStyle("-fx-background-color: " + hex + ";");
                    updateAllTicketHeaders(hex);
                });
            }
        } catch (Exception e) {}
    }

    private void updateAllTicketHeaders(String hexColor) {
        if (ticketGrid == null) return;
        for (Node node : ticketGrid.getChildren()) {
            if (node instanceof VBox) {
                VBox ticket = (VBox) node;
                if (!ticket.getChildren().isEmpty() && ticket.getChildren().get(0) instanceof HBox) {
                    HBox header = (HBox) ticket.getChildren().get(0);
                    header.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 8 8 0 0;");
                }
            }
        }
    }

    private void loadMenuData() {
        File menuFile = new File(DATA_DIR, MENU_FILE);
        if (menuFile.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(menuFile))) {
                fullMenu = (List<Order>) in.readObject();
            } catch (Exception e) { fullMenu = new ArrayList<>(); }
        } else {
            fullMenu = new ArrayList<>();
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            out.flush();
            while (!clientSocket.isClosed()) {
                try {
                    Object received = in.readObject();
                    if (received instanceof OrderPayload) {
                        OrderPayload order = (OrderPayload) received;
                        Platform.runLater(() -> addTicket(order));
                    } else if (received instanceof String) {
                        String command = (String) received;
                        if (command.startsWith("LOGIN:")) {
                            String inputPass = command.substring(6);
                            boolean isAuthenticated = checkPassword(inputPass);
                            out.writeBoolean(isAuthenticated);
                            out.flush();
                        } else {
                            handleCommand(command, out);
                        }
                    }
                } catch (EOFException e) { break; }
            }
        } catch (Exception e) {
        } finally { try { clientSocket.close(); } catch (Exception e) {} }
    }

    private boolean checkPassword(String input) {
        try {
            File passFile = new File(DATA_DIR, PASS_FILE);
            if (passFile.exists()) {
                String storedPass = Files.readString(passFile.toPath()).trim();
                return storedPass.equals(input);
            }
            return input.equals("admin");
        } catch (IOException e) { return false; }
    }

    private void handleCommand(String command, ObjectOutputStream out) throws IOException {
        if (command.startsWith("GET_IMAGE_DATA:")) {
            String requestedFile = command.substring("GET_IMAGE_DATA:".length());
            File file = new File(DATA_DIR, requestedFile);
            if (file.exists()) {
                out.writeObject(Files.readAllBytes(file.toPath()));
            } else {
                out.writeObject(null);
            }
            out.flush();
            return;
        }

        switch (command) {
            case "GET_ICON":
                File iconFile = new File(DATA_DIR, ICON_FILE);
                out.writeObject(iconFile.exists() ? Files.readAllBytes(iconFile.toPath()) : null);
                break;
            case "GET_ICON_TIMESTAMP": out.writeLong(iconLastModified); break;
            case "GET_NAME":
                File nameFile = new File(DATA_DIR, NAME_FILE);
                out.writeObject(nameFile.exists() ? Files.readString(nameFile.toPath()) : "Dineamic");
                break;
            case "GET_NAME_TIMESTAMP": out.writeLong(nameLastModified); break;
            case "GET_MENU": out.writeObject(fullMenu); break;
            case "GET_MENU_TIMESTAMP": out.writeLong(menuLastModified); break;
            case "GET_COLOR":
                File colorFile = new File(DATA_DIR, COLOR_FILE);
                out.writeObject(colorFile.exists() ? Files.readString(colorFile.toPath()) : "#4338CA");
                break;
            case "GET_COLOR_TIMESTAMP": out.writeLong(colorLastModified); break;
        }
        out.flush();
    }

    private void addTicket(OrderPayload order) {
        VBox ticket = new VBox();
        ticket.setPrefWidth(220);
        ticket.getStyleClass().add("kitchen-ticket");

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("ticket-header");
        header.setStyle("-fx-background-color: " + currentThemeColor + "; -fx-background-radius: 8 8 0 0;");

        Label tableLabel = new Label("Table " + order.getTableNumber());
        tableLabel.setFont(Font.font("System Bold", 18));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Label timeLabel = new Label(sdf.format(new Date(order.getTimestamp())));
        timeLabel.setFont(Font.font("System Bold", 16));
        timeLabel.setStyle("-fx-text-fill: #FFFFFF;");

        header.getChildren().addAll(tableLabel, spacer, timeLabel);

        VBox body = new VBox(8);
        body.setPadding(new Insets(10));
        VBox.setVgrow(body, Priority.ALWAYS);

        Map<String, Integer> counts = new HashMap<>();
        for (Order item : order.getItems()) counts.put(item.getName(), counts.getOrDefault(item.getName(), 0) + 1);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String text = entry.getValue() + "x " + entry.getKey();
            Label itemLabel = new Label(text);
            itemLabel.getStyleClass().add("order-item-text");
            body.getChildren().add(itemLabel);
        }

        Button finishBtn = new Button("Finish Order");
        finishBtn.setMaxWidth(Double.MAX_VALUE);
        finishBtn.setPrefHeight(40);
        finishBtn.getStyleClass().add("finish-btn");
        finishBtn.setOnAction(e -> {
            ticketGrid.getChildren().remove(ticket);
            totalOrdersLabel.setText("Orders: " + ticketGrid.getChildren().size());
        });

        ticket.getChildren().addAll(header, body, finishBtn);
        ticketGrid.getChildren().add(ticket);
        totalOrdersLabel.setText("Orders: " + ticketGrid.getChildren().size());
    }

    public void shutdown() { isRunning = false; try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {} }
}