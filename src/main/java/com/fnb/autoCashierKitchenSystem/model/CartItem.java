package com.fnb.autoCashierKitchenSystem.model;

public class CartItem {
    private final Order menuItem;
    private int quantity;

    public CartItem(Order menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public Order getOrder() { return menuItem; }
    public int getQuantity() { return quantity; }
    public void increment() { this.quantity++; }
    public double getTotalPrice() { return menuItem.getPrice() * quantity; }
}

