package com.datingapp;

import com.datingapp.client.services.WebSocketClientService;
import com.datingapp.client.ui.ChatController; // Make sure this is imported
import com.datingapp.client.ui.LoginController;
import com.datingapp.client.ui.RegistrationController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainClient extends Application {

    private Stage primaryStage;
    private WebSocketClientService webSocketService;
    private Object currentController; // Keep track of current controller for cleanup

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Dating App");

        this.webSocketService = new WebSocketClientService();
        // Connect to ws://localhost:8025/websockets/chat/guest (or similar general endpoint)
        // The ChatServerEndpoint @OnOpen uses @PathParam("username") which is "guest" here.
        // Actual user login will happen via messages.
        webSocketService.connect(); 

        showLoginScreen();
    }
    
    private void cleanupPreviousController() {
        if (currentController instanceof ChatController) {
            ((ChatController) currentController).cleanUp();
        }
        // Add similar blocks if LoginController or RegistrationController need cleanup
        // For now, LoginController and RegistrationController do not have specific cleanup,
        // but their message handlers might need to be cleared if they become more complex
        // and to prevent multiple handlers from acting on the same messages after view switch.
        // Example: if LoginController also had a specific message handler:
        // else if (currentController instanceof LoginController) {
        //    ((LoginController) currentController).cleanUp(); // Assuming LoginController gets a cleanUp method
        // }
    }

    public void showLoginScreen() {
        cleanupPreviousController();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setMainApp(this);
            controller.setWebSocketService(webSocketService);
            currentController = controller;

            Scene scene = new Scene(root, 400, 350);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Dating App - Login");
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRegistrationScreen() {
        cleanupPreviousController();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Registration.fxml"));
            Parent root = loader.load();

            RegistrationController controller = loader.getController();
            controller.setMainApp(this);
            controller.setWebSocketService(webSocketService);
            currentController = controller;
            
            Scene scene = new Scene(root, 400, 450);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Dating App - Register");
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void showChatView(String username) {
        cleanupPreviousController();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/ChatView.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            controller.setMainApp(this);
            controller.setWebSocketService(webSocketService);
            controller.setUsername(username); // Pass the username
            currentController = controller;

            Scene scene = new Scene(root, 750, 500); // Adjusted size for chat view
            primaryStage.setScene(scene);
            primaryStage.setTitle("Chat - " + username);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load ChatView.fxml: " + e.getMessage());
        }
    }

    @Override
    public void stop() throws Exception {
        cleanupPreviousController(); // Cleanup current controller before exit
        if (webSocketService != null) {
            webSocketService.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public Stage getPrimaryStage() { 
        return primaryStage; 
    }
}
