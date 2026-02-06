package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

/**
 * Model class representing a single item in the user's moving inventory.
 * Implements Serializable to allow passing objects between Android components.
 */
public class InventoryItem implements Serializable {

    private String id;          // Firestore Document ID (Excluded from DB payload)
    private String ownerId;     // The user who owns this item
    private String name;        // e.g., "Books", "Sofa"
    private String description; // e.g., "Very heavy boxes", "Glass cabinet"
    private String roomType;    // e.g., "Living Room", "Bedroom"
    private boolean isFragile;  // Flag indicating if special care is needed
    private int quantity;       // Quantity of items
    private String imageUrl;    // Firebase Storage URL for the item image
    private long createdAt;     // Timestamp of creation

    /**
     * Empty constructor required for Firestore serialization.
     */
    public InventoryItem() {
        // Default constructor
    }

    /**
     * Full constructor for creating new items.
     */
    public InventoryItem(String ownerId, String name, String description, String roomType,
                         boolean isFragile, int quantity, String imageUrl) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.roomType = roomType;
        this.isFragile = isFragile;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.createdAt = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    /**
     * @return The Firestore Document ID.
     * Annotated with @Exclude so this ID is not saved as a field INSIDE the document.
     */
    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isFragile() {
        return isFragile;
    }

    public void setFragile(boolean fragile) {
        isFragile = fragile;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}