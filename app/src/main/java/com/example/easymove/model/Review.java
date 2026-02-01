package com.example.easymove.model;

public class Review {

    private String moverId;          // המוביל שמדורג
    private String reviewerId;       // מי כתב את הביקורת
    private String reviewerName;     // שם הלקוח
    private int stars;               // 1–5
    private String text;             // תוכן הביקורת
    private long createdAt;          // זמן

    public Review() {
        // חובה לפיירבייס
    }

    public Review(String moverId, String reviewerId, String reviewerName, int stars, String text) {
        this.moverId = moverId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.stars = stars;
        this.text = text;
        this.createdAt = System.currentTimeMillis();
    }

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
    public void setMoverId(String moverId) { this.moverId = moverId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public void setStars(int stars) { this.stars = stars; }
    public void setText(String text) { this.text = text; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }


    public long getCreatedAt() {
        return createdAt;
    }
}
