package com.datingapp.client.ui;

import com.datingapp.MainClient;
import com.datingapp.client.services.WebSocketClientService;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegistrationController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    
    private WebSocketClientService webSocketService;
    private MainClient mainClientApp;

    public void setMainApp(MainClient mainClientApp) {
        this.mainClientApp = mainClientApp;
    }

    public void setWebSocketService(WebSocketClientService service) {
        this.webSocketService = service;
        // Define how to handle messages from the server related to registration
         this.webSocketService.addMessageHandler(message -> {
            Platform.runLater(() -> {
                if (message.startsWith("REGISTER_SUCCESS:")) {
                    statusLabel.getStyleClass().setAll("label", "status-label-success");
                    statusLabel.setText(message.substring("REGISTER_SUCCESS:".length()));
                    // Optionally clear fields or navigate to login
                } else if (message.startsWith("REGISTER_FAIL:")) {
                    statusLabel.getStyleClass().setAll("label", "status-label-error");
                    statusLabel.setText(message.substring("REGISTER_FAIL:".length()));
                }
            });
        });
    }

    @FXML
    private void handleRegisterButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.getStyleClass().setAll("label", "status-label-error");
            statusLabel.setText("All fields are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.getStyleClass().setAll("label", "status-label-error");
            statusLabel.setText("Passwords do not match.");
            return;
        }
        // Basic email validation (optional, can be more complex)
        if (!email.contains("@")) { // Contains basic check
            statusLabel.getStyleClass().setAll("label", "status-label-error");
            statusLabel.setText("Invalid email format.");
            return;
        }

        statusLabel.getStyleClass().setAll("label"); // Reset to default label style
        statusLabel.setText("Attempting registration...");
        if (webSocketService != null && webSocketService.isConnected()) {
            webSocketService.sendMessage("REGISTER:" + username + ":" + password + ":" + email);
        } else {
            statusLabel.setText("Not connected to server. Please try again later.");
        }
    }

    @FXML
    private void handleLoginLinkAction(ActionEvent event) {
         if (mainClientApp != null) {
            mainClientApp.showLoginScreen();
        }
    }
}
