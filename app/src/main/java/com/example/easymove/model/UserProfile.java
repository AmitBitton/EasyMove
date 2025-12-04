package com.example.easymove.model;

import java.util.List;

/**
 * UserProfile – represents a user profile in the EasyMove app.
 *
 * There are two user types:
 *  - "customer" – user who orders a move
 *  - "mover"    – user who provides moving services
 *
 * The same model is used for both types.
 * Fields that are not relevant for a specific type can remain null.
 */
public class UserProfile {

    /* -----------------------  General user fields  ----------------------- */

    private String userId;            // Firebase user ID
    private String name;              // Full name
    private String phone;             // Phone number
    private String userType;          // "customer" or "mover"
    private String profileImageUrl;   // Profile image URL stored in Firebase Storage

    /* -----------------------  Default move addresses  ----------------------- */

    // Textual addresses chosen by the user (optional)
    private String defaultFromAddress;  // Default origin address (where the move starts)
    private String defaultToAddress;    // Default destination address (where the move ends)

    // Coordinates for integration with Google Maps (optional but useful)
    private Double fromLat;             // Latitude for default origin
    private Double fromLng;             // Longitude for default origin
    private Double toLat;               // Latitude for default destination
    private Double toLng;               // Longitude for default destination

    /* -----------------------  Mover-specific fields  ----------------------- */

    // List of areas where the mover works, e.g. ["Center", "Jerusalem"]
    private List<String> serviceAreas;

    // Professional description – shown only for movers
    private String about;

    // Rating data for movers (0 if no ratings yet)
    private float rating;              // Average rating value
    private int ratingCount;           // Number of ratings received

    /* -----------------------  Empty constructor for Firebase  ----------------------- */

    public UserProfile() {
        // Required empty constructor for Firestore deserialization
    }

    /* -----------------------  Full constructor (optional to use)  ----------------------- */

    public UserProfile(String userId,
                       String name,
                       String phone,
                       String userType,
                       String profileImageUrl,
                       String defaultFromAddress,
                       String defaultToAddress,
                       Double fromLat,
                       Double fromLng,
                       Double toLat,
                       Double toLng,
                       List<String> serviceAreas,
                       String about,
                       float rating,
                       int ratingCount) {

        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.userType = userType;
        this.profileImageUrl = profileImageUrl;

        this.defaultFromAddress = defaultFromAddress;
        this.defaultToAddress = defaultToAddress;

        this.fromLat = fromLat;
        this.fromLng = fromLng;
        this.toLat = toLat;
        this.toLng = toLng;

        this.serviceAreas = serviceAreas;
        this.about = about;

        this.rating = rating;
        this.ratingCount = ratingCount;
    }

    /* -----------------------  Getters & setters  ----------------------- */

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

    public List<String> getServiceAreas() { return serviceAreas; }
    public void setServiceAreas(List<String> serviceAreas) { this.serviceAreas = serviceAreas; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
}
