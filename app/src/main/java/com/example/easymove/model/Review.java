package com.example.easymove.model;

import java.io.Serializable;

/**
 * Model class representing a review and rating left by a Customer for a Mover.
 * Stored in Firestore (typically in a 'reviews' root collection or sub-collection).
 */
public class Review implements Serializable {

    private String moverId;          // The ID of the Mover being rated
    private String reviewerId;       // The ID of the Customer writing the review
    private String reviewerName;     // The display name of the Customer
    private int stars;               // Rating value (1-5)
    private String text;             // The review content/comment
    private long createdAt;          // Timestamp of creation

    /**
     * Default constructor required for Firestore serialization.
     */
    public Review() {
    }

    /**
     * Constructor for creating a new review.
     *
     * @param moverId      The ID of the mover.
     * @param reviewerId   The ID of the customer.
     * @param reviewerName The name of the customer.
     * @param stars        The star rating (1-5).
     * @param text         The review text.
     */
    public Review(String moverId, String reviewerId, String reviewerName, int stars, String text) {
        this.moverId = moverId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.stars = stars;
        this.text = text;
        this.createdAt = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    public String getMoverId() {
        return moverId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public int getStars() {
        return stars;
    }

    public String getText() {
        return text;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // ------------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------------

    public void setMoverId(String moverId) {
        this.moverId = moverId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}