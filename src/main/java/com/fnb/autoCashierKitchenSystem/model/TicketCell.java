package com.fnb.autoCashierKitchenSystem.model;

import com.fnb.autoCashierKitchenSystem.controllers.CashierController;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class TicketCell extends ListCell<CartItem> {
    private final HBox content;
    private final Label quantityLabel; // New label for "2x"
    private final Label nameLabel;
    private final Label priceLabel;

    public TicketCell() {
        // 1. Quantity Label (Leftmost)
        quantityLabel = new Label();
        quantityLabel.setPrefWidth(30); // Fixed width for alignment
        quantityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4f46e5;");

        // 2. Name Label
        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

        // 3. Price Label (Displays Total for that line: 2 * Price)
        priceLabel = new Label();
        priceLabel.setStyle("-fx-text-fill: #374151;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 4. Delete Button
        Button deleteButton = new Button("🗑");
        deleteButton.getStyleClass().add("delete-btn");
        deleteButton.setOnAction(event -> {
            CartItem itemToRemove = getItem();
            if (itemToRemove != null) {
                CashierController.currentOrder.remove(itemToRemove);
            }
        });

        // Layout: [Qty] [Name] ....spacer.... [Price] [Delete]
        content = new HBox(5);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(quantityLabel, nameLabel, spacer, priceLabel, deleteButton);
    }

    @Override
    protected void updateItem(CartItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            quantityLabel.setText(item.getQuantity() + "x");
            nameLabel.setText(item.getOrder().getName());
            priceLabel.setText(String.format("$%.2f", item.getTotalPrice()));
            setGraphic(content);
        }
    }
}
