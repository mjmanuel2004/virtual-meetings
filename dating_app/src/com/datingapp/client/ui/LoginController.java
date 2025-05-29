package com.datingapp.client.ui;

import com.datingapp.MainClient; // Will be used to switch scenes
import com.datingapp.client.services.WebSocketClientService; // To send messages

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private WebSocketClientService webSocketService;
    private MainClient mainClientApp; // To call scene switching methods

    public void initialize() {
        // Get the WebSocket service instance (e.g., from MainClient or a singleton)
        // For now, let's assume it will be injected or set by MainClient
    }
    
    public void setMainApp(MainClient mainClientApp) {
        this.mainClientApp = mainClientApp;
    }

    public void setWebSocketService(WebSocketClientService service) {
        this.webSocketService = service;
        // Define how to handle messages from the server related to login
        this.webSocketService.addMessageHandler(message -> {
            Platform.runLater(() -> {
                if (message.startsWith("LOGIN_SUCCESS:")) {
                    statusLabel.getStyleClass().setAll("label", "status-label-success");
                    statusLabel.setText(message.substring("LOGIN_SUCCESS:".length()));
                    // Navigate to chat view (to be implemented in MainClient)
                    if (mainClientApp != null) {
                        mainClientApp.showChatView(usernameField.getText());
                    }
                } else if (message.startsWith("LOGIN_FAIL:")) {
                    statusLabel.getStyleClass().setAll("label", "status-label-error");
                    statusLabel.setText(message.substring("LOGIN_FAIL:".length()));
                }
            });
        });
    }


    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.getStyleClass().setAll("label", "status-label-error");
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }
        statusLabel.getStyleClass().setAll("label"); // Reset to default label style
        statusLabel.setText("Attempting login...");
        if (webSocketService != null && webSocketService.isConnected()) {
            webSocketService.sendMessage("LOGIN:" + username + ":" + password);
        } else {
            statusLabel.setText("Not connected to server. Please try again later.");
        }
    }

    @FXML
    private void handleRegisterLinkAction(ActionEvent event) {
        if (mainClientApp != null) {
            mainClientApp.showRegistrationScreen();
        }
    }
}
