package com.fnb.autoCashierKitchenSystem.model;

import java.io.Serializable;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private double price;
    private String category; // "Food" or "Beverage"
    private String imagePath;

    public Order(String name, double price, String category, String imagePath) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.imagePath = imagePath;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImagePath() { return imagePath; }

    @Override
    public String toString() {
        return name; // Useful for debugging
    }
}