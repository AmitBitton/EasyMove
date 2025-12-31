package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class InventoryItem implements Serializable {
    private String id;
    private String ownerId;
    private String name;        // למשל: "ספרים"
    private String description; // למשל: "כבד מאוד"
    private String roomType;    // למשל: "סלון"
    private boolean isFragile;  // שביר?
    private int quantity;       // כמות
    private String imageUrl;    // URL לתמונה
    private long createdAt;

    public InventoryItem() {} // חובה עבור Firebase

    public InventoryItem(String ownerId, String name, String description, String roomType, boolean isFragile, int quantity, String imageUrl) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.roomType = roomType;
        this.isFragile = isFragile;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public boolean isFragile() { return isFragile; }
    public void setFragile(boolean fragile) { isFragile = fragile; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getCreatedAt() { return createdAt; }
}