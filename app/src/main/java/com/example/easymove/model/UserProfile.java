package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.List;

/**
 * Model class representing a user in the system.
 * This class serves as a unified profile for both "Customer" and "Mover" roles.
 * It acts as the Source of Truth for default addresses, contact info, and preferences.
 */
public class UserProfile implements Serializable {

    // ------------------------------------------------------------------------
    // Personal Information (Common)
    // ------------------------------------------------------------------------
    private String userId;
    private String name;
    private String phone;
    private String userType;          // "customer" or "mover"
    private String profileImageUrl;
    private String email;

    // ------------------------------------------------------------------------
    // Customer Specific Fields
    // ------------------------------------------------------------------------

    // Textual addresses (What the user sees)
    private String defaultFromAddress;
    private String defaultToAddress;

    // Coordinates for addresses (Used for distance calculations)
    private Double fromLat;
    private Double fromLng;
    private Double toLat;
    private Double toLng;

    // Apartment specific details
    private Integer floor;
    private Integer apartment;

    // The preferred move date (Synced with the active move request)
    private Long defaultMoveDate;

    // ------------------------------------------------------------------------
    // Mover Specific Fields
    // ------------------------------------------------------------------------

    // Base location for geographical search (GeoHash + Lat/Lng)
    private String geohash;
    private double lat;
    private double lng;

    // Operational radius in Kilometers
    private int serviceRadiusKm = 30;

    // List of service area names (e.g., "Tel Aviv", "Haifa")
    private List<String> serviceAreas;

    // Business details
    private String about;
    private float rating;
    private int ratingCount;

    // ------------------------------------------------------------------------
    // System & Metadata
    // ------------------------------------------------------------------------

    // FCM Token for Push Notifications
    private String fcmToken;

    // Runtime helper field for sorting results by distance.
    // Not stored in the database.
    private double distanceFromUser;

    /**
     * Default constructor required for Firestore serialization.
     */
    public UserProfile() {
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    // --- Personal Info ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    // --- Customer Fields ---

    public String getDefaultFromAddress() { return defaultFromAddress; }
    public void setDefaultFromAddress(String defaultFromAddress) { this.defaultFromAddress = defaultFromAddress; }

    public String getDefaultToAddress() { return defaultToAddress; }
    public void setDefaultToAddress(String defaultToAddress) { this.defaultToAddress = defaultToAddress; }

    public Double getFromLat() { return fromLat; }
    public void setFromLat(Double fromLat) { this.fromLat = fromLat; }

    public Double getFromLng() { return fromLng; }
    public void setFromLng(Double fromLng) { this.fromLng = fromLng; }

    public Double getToLat() { return toLat; }
    public void setToLat(Double toLat) { this.toLat = toLat; }

    public Double getToLng() { return toLng; }
    public void setToLng(Double toLng) { this.toLng = toLng; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public Integer getApartment() { return apartment; }
    public void setApartment(Integer apartment) { this.apartment = apartment; }

    public Long getDefaultMoveDate() { return defaultMoveDate; }
    public void setDefaultMoveDate(Long defaultMoveDate) { this.defaultMoveDate = defaultMoveDate; }

    // --- Mover Fields ---

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public int getServiceRadiusKm() { return serviceRadiusKm; }
    public void setServiceRadiusKm(int serviceRadiusKm) { this.serviceRadiusKm = serviceRadiusKm; }

    public List<String> getServiceAreas() { return serviceAreas; }
    public void setServiceAreas(List<String> serviceAreas) { this.serviceAreas = serviceAreas; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // --- System Fields ---

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    /**
     * @return The calculated distance from the searching user.
     * Annotated with @Exclude so it is NOT saved to Firestore.
     */
    @Exclude
    public double getDistanceFromUser() { return distanceFromUser; }

    @Exclude
    public void setDistanceFromUser(double distanceFromUser) { this.distanceFromUser = distanceFromUser; }
}