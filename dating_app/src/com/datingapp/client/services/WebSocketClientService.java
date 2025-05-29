package com.datingapp.client.services;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ClientEndpoint
public class WebSocketClientService {

    private Session session;
    private String serverUriBase = "ws://localhost:8025/websockets/chat/"; // Matches conceptual server
    private List<Consumer<String>> messageHandlers = new ArrayList<>();

    public WebSocketClientService() {
        // Constructor can be expanded if needed
    }

    public void connect(String username) { // Username might be passed to connect to a specific endpoint
        if (session == null || !session.isOpen()) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                // The URI might need to be dynamic if username is part of path from start
                // Or connect to a generic endpoint and then send LOGIN message with username
                String uri = serverUriBase + username; // Assuming server uses /chat/{username}
                System.out.println("Connecting to: " + uri);
                this.session = container.connectToServer(this, URI.create(uri));
            } catch (Exception e) {
                System.err.println("Error connecting to WebSocket: " + e.getMessage());
                e.printStackTrace();
                // Notify UI of connection failure
                 messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Connection_Failed:" + e.getMessage()));
            }
        }
    }
    
    // Alternative connect for endpoints that don't take username in path initially
    public void connect() {
         if (session == null || !session.isOpen()) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                // Connect to a general endpoint, then authenticate
                String uri = "ws://localhost:8025/websockets/chat/guest"; // Example general endpoint
                System.out.println("Connecting to: " + uri);
                this.session = container.connectToServer(this, URI.create(uri));
            } catch (Exception e) {
                 System.err.println("Error connecting to WebSocket: " + e.getMessage());
                e.printStackTrace();
                messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Connection_Failed:" + e.getMessage()));
            }
        }
    }


    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connected to WebSocket server. Session ID: " + session.getId());
        // Notify UI or other parts of successful connection
        messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Connected_Successfully"));
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Message received from server: " + message);
        messageHandlers.forEach(handler -> handler.accept(message));
    }

    @OnClose
    public void onClose(Session session) {
        this.session = null;
        System.out.println("Disconnected from WebSocket server.");
         messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Disconnected"));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
        throwable.printStackTrace();
        // Optionally try to reconnect or notify user
        messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Error:" + throwable.getMessage()));
    }

    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                System.out.println("Message sent to server: " + message);
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Cannot send message, session is not active.");
             messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Not_Connected_Cannot_Send"));
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    public void addMessageHandler(Consumer<String> handler) {
        this.messageHandlers.add(handler);
    }
    
    public void removeMessageHandler(Consumer<String> handler) {
        this.messageHandlers.remove(handler);
    }

    public void close() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
