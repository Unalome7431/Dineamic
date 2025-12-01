package com.fnb.autoCashierKitchenSystem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OrderPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private int tableNumber;
    private List<Order> items;
    private long timestamp;

    public OrderPayload(int tableNumber, List<Order> items) {
        this.tableNumber = tableNumber;
        this.items = new ArrayList<>(items); // Copy list to ensure serializability
        this.timestamp = System.currentTimeMillis();
    }

    public int getTableNumber() { return tableNumber; }
    public List<Order> getItems() { return items; }
    public long getTimestamp() { return timestamp; }
}