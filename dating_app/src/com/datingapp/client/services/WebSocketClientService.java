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
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

// Dans WebSocketClientService.java
// Remplacer la ligne avec serverUriBase par :

@ClientEndpoint
public class WebSocketClientService {

    private Session session;
    private String serverUriBase;
    private List<Consumer<String>> messageHandlers = new ArrayList<>();

    public WebSocketClientService() {
        // Lire l'adresse du serveur depuis les propriétés système
        String serverHost = System.getProperty("server.host", "localhost");
        String serverPort = System.getProperty("server.port", "8025");
        this.serverUriBase = "ws://" + serverHost + ":" + serverPort + "/websockets/chat/";

        System.out.println("Configuration du client WebSocket :");
        System.out.println("  Host du serveur : " + serverHost);
        System.out.println("  Port du serveur : " + serverPort);
        System.out.println("  URI de base : " + serverUriBase);
    }

    // Alternative : méthode pour définir l'adresse manuellement
    public void setServerAddress(String host, int port) {
        this.serverUriBase = "ws://" + host + ":" + port + "/websockets/chat/";
        System.out.println("Adresse du serveur mise à jour : " + serverUriBase);
    }

    // Le reste du code reste identique...
    public void connect(String username) {
        if (session == null || !session.isOpen()) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                String uri = serverUriBase + username;
                System.out.println("Tentative de connexion à : " + uri);
                this.session = container.connectToServer(this, URI.create(uri));
            } catch (Exception e) {
                System.err.println("Erreur lors de la connexion WebSocket : " + e.getMessage());
                e.printStackTrace();
                messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Connection_Failed:" + e.getMessage()));
            }
        }
    }

    public void connect() {
        if (session == null || !session.isOpen()) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                String uri = serverUriBase + "guest";
                System.out.println("Tentative de connexion à : " + uri);
                this.session = container.connectToServer(this, URI.create(uri));
            } catch (Exception e) {
                System.err.println("Erreur lors de la connexion WebSocket : " + e.getMessage());
                e.printStackTrace();
                messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Connection_Failed:" + e.getMessage()));
            }
        }
    }

    // Reste du code inchangé...
}