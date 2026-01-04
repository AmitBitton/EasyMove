package com.example.easymove.model;

import java.util.List;

/**
 * UserProfile
 * מייצג משתמש (לקוח או מוביל).
 * משמש כמקור האמת (Source of Truth) לכתובות ברירת מחדל ולתאריך מועדף.
 */
public class UserProfile {

    /* -----------------------  פרטים אישיים  ----------------------- */
    private String userId;
    private String name;
    private String phone;
    private String userType;          // "customer" or "mover"
    private String profileImageUrl;

    /* -----------------------  שדות ללקוח (Customer)  ----------------------- */
    // כתובות טקסטואליות (מה שהמשתמש רואה)
    private String defaultFromAddress;
    private String defaultToAddress;

    // קואורדינטות לכתובות הלקוח (לחישוב מרחקים עתידי)
    private Double fromLat;
    private Double fromLng;
    private Double toLat;
    private Double toLng;

    // פרטי דירה
    private Integer floor;
    private Integer apartment;

    // ✅ התאריך הקובע! סנכרון מול ההובלה הפעילה
    private Long defaultMoveDate;

    /* -----------------------  שדות למוביל (Mover)  ----------------------- */
    // מיקום בסיס של המוביל (לחיפוש גיאוגרפי)
    private String geohash;
    private double lat;
    private double lng;

    // רדיוס שירות
    private int serviceRadiusKm = 30;

    // אזורי שירות (טקסט)
    private List<String> serviceAreas;

    // אודות ודירוג
    private String about;
    private float rating;
    private int ratingCount;

    // טוקן להתראות
    private String fcmToken;

    // שדה עזר (לא נשמר במסד, מחושב בזמן ריצה למיון)
    private double distanceFromUser;

    /* -----------------------  Constructors  ----------------------- */

    public UserProfile() { } // חובה לפיירבייס

    /* -----------------------  Getters & Setters  ----------------------- */

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    // --- לקוח ---
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

    // --- מוביל ---
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

    public double getDistanceFromUser() { return distanceFromUser; }
    public void setDistanceFromUser(double distanceFromUser) { this.distanceFromUser = distanceFromUser; }
}