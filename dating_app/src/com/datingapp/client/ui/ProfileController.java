package com.datingapp.client.ui;

import com.datingapp.client.model.UserProfile; 
import com.datingapp.client.services.WebSocketClientService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ProfileController {

    @FXML private TextField avatarUrlField;
    @FXML private Button saveAvatarButton; // Renamed
    @FXML private TextArea bioTextArea;    // New
    @FXML private Button saveBioButton;      // New
    @FXML private Button closeButton;
    @FXML private Label statusLabel;

    private WebSocketClientService webSocketService;
    private Stage dialogStage;
    private UserProfile currentUserProfile; // Store the whole profile
    private boolean profileDataChanged = false;


    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setWebSocketService(WebSocketClientService webSocketService) {
        this.webSocketService = webSocketService;
        // Add a single handler for all profile-related server messages
        this.webSocketService.addMessageHandler(this::handleServerProfileMessages);
    }

    // Method to set current user data
    public void setUserProfile(UserProfile userProfile) {
        this.currentUserProfile = userProfile;
        if (userProfile != null) {
            avatarUrlField.setText(userProfile.getAvatarUrl() != null ? userProfile.getAvatarUrl() : "");
            bioTextArea.setText(userProfile.getBio() != null ? userProfile.getBio() : "");
        }
    }
    
    public boolean isProfileDataChanged() {
        return profileDataChanged;
    }

    // Getter for the updated profile, to be called by ChatController
    public UserProfile getUpdatedUserProfile() {
        return currentUserProfile;
    }

    private void handleServerProfileMessages(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("AVATAR_UPDATE_SUCCESS:")) {
                statusLabel.getStyleClass().setAll("label","status-label-success");
                statusLabel.setText("Avatar URL updated successfully!");
                String newAvatarUrl = message.substring("AVATAR_UPDATE_SUCCESS:".length());
                if (currentUserProfile != null) currentUserProfile.setAvatarUrl(newAvatarUrl);
                avatarUrlField.setText(newAvatarUrl); // ensure field shows the saved one
                profileDataChanged = true; 
            } else if (message.startsWith("AVATAR_UPDATE_FAIL:")) {
                statusLabel.getStyleClass().setAll("label","status-label-error");
                statusLabel.setText("Avatar Error: " + message.substring("AVATAR_UPDATE_FAIL:".length()));
            } else if (message.startsWith("PROFILE_UPDATE_SUCCESS:")) { 
                String jsonPayload = message.substring("PROFILE_UPDATE_SUCCESS:".length());
                String newBio = "";
                // Basic JSON parsing for {"bio":"content"}
                if (jsonPayload.startsWith("{\"bio\":\"") && jsonPayload.endsWith("\"}")) {
                     newBio = jsonPayload.substring("{\"bio\":\"".length(), jsonPayload.length() - 2);
                     // Unescape basic characters that server might have escaped for JSON
                     newBio = newBio.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "").replace("\\t", "\t");
                } else {
                    System.err.println("Error parsing bio from PROFILE_UPDATE_SUCCESS: " + jsonPayload);
                }
                statusLabel.getStyleClass().setAll("label","status-label-success");
                statusLabel.setText("Profile bio updated successfully!");
                if (currentUserProfile != null) currentUserProfile.setBio(newBio);
                bioTextArea.setText(newBio); 
                profileDataChanged = true;
            } else if (message.startsWith("PROFILE_UPDATE_FAIL:")) {
                statusLabel.getStyleClass().setAll("label","status-label-error");
                statusLabel.setText("Profile Error: " + message.substring("PROFILE_UPDATE_FAIL:".length()));
            }
        });
    }

    @FXML
    private void handleSaveAvatarButtonAction(ActionEvent event) {
        String newAvatarUrl = avatarUrlField.getText().trim();
        if (newAvatarUrl.length() > 512 || (!newAvatarUrl.isEmpty() && !newAvatarUrl.matches("^https?://.*\.(png|jpg|jpeg|gif|webp)$"))) {
             statusLabel.getStyleClass().setAll("label","status-label-error");
             statusLabel.setText("Invalid Avatar URL format, extension, or too long.");
             return;
        }
        if (webSocketService != null && webSocketService.isConnected()) {
            statusLabel.getStyleClass().setAll("label"); // Reset status label style
            statusLabel.setText("Saving avatar URL...");
            webSocketService.sendMessage("UPDATE_AVATAR_URL:" + newAvatarUrl);
        } else { 
            statusLabel.getStyleClass().setAll("label","status-label-error");
            statusLabel.setText("Not connected to server.");
        }
    }

    @FXML
    private void handleSaveBioButtonAction(ActionEvent event) {
        String newBio = bioTextArea.getText().trim();
        if (newBio.length() > 2000) { 
            statusLabel.getStyleClass().setAll("label","status-label-error");
            statusLabel.setText("Bio is too long (max 2000 characters).");
            return;
        }
        
        // Escape for JSON: quotes, backslashes, newlines, tabs, carriage returns
        String escapedBio = newBio.replace("\\", "\\\\")
                                  .replace("\"", "\\\"")
                                  .replace("\n", "\\n")
                                  .replace("\r", "\\r")
                                  .replace("\t", "\\t");
        String clientJsonPayload = "{\"bio\":\"" + escapedBio + "\"}";

        if (webSocketService != null && webSocketService.isConnected()) {
            statusLabel.getStyleClass().setAll("label"); // Reset status label style
            statusLabel.setText("Saving bio...");
            webSocketService.sendMessage("UPDATE_PROFILE:" + clientJsonPayload);
        } else {
            statusLabel.getStyleClass().setAll("label","status-label-error");
            statusLabel.setText("Not connected to server.");
        }
    }

    @FXML
    private void handleCloseButtonAction(ActionEvent event) { 
        cleanup(); if (dialogStage != null) { dialogStage.close(); }
    }
    
    public void cleanup() { 
         if (webSocketService != null) { webSocketService.removeMessageHandler(this::handleServerProfileMessages); }
    }
}
