package com.datingapp.client.model;

public class UserProfile {
    private final String username;
    private String avatarUrl;
    private String bio; // New field for biography

    // Constructor for when bio might not be immediately available
    public UserProfile(String username, String avatarUrl) {
        this.username = username;
        this.avatarUrl = (avatarUrl == null || avatarUrl.trim().isEmpty()) ? null : avatarUrl.trim();
        this.bio = null; // Initialize bio as null or empty string
    }

    // Overloaded constructor for when all details are available
    public UserProfile(String username, String avatarUrl, String bio) {
        this.username = username;
        this.avatarUrl = (avatarUrl == null || avatarUrl.trim().isEmpty()) ? null : avatarUrl.trim();
        this.bio = (bio == null || bio.trim().isEmpty()) ? null : bio.trim();
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = (avatarUrl == null || avatarUrl.trim().isEmpty()) ? null : avatarUrl.trim();
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = (bio == null || bio.trim().isEmpty()) ? null : bio.trim();
    }

    @Override
    public String toString() {
        return "UserProfile{" +
               "username='" + username + '\'' +
               ", avatarUrl='" + avatarUrl + '\'' +
               ", bio='" + (bio != null ? bio.substring(0, Math.min(bio.length(), 30))+"..." : "null") + '\'' + // Display snippet of bio
               '}';
    }
}
