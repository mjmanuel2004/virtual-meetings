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
    private String serverUriBase;
    private List<Consumer<String>> messageHandlers = new ArrayList<>();

    public WebSocketClientService() {
        // Lire l'adresse du serveur depuis les propriétés système
        String serverHost = System.getProperty("server.host", "192.168.1.7");
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

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connecté au serveur WebSocket. ID de session : " + session.getId());
        messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Connected_Successfully"));
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Message reçu du serveur : " + message);
        messageHandlers.forEach(handler -> handler.accept(message));
    }

    @OnClose
    public void onClose(Session session) {
        this.session = null;
        System.out.println("Déconnecté du serveur WebSocket.");
        messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Disconnected"));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Erreur WebSocket : " + throwable.getMessage());
        throwable.printStackTrace();
        messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Error:" + throwable.getMessage()));
    }

    // MÉTHODES MANQUANTES - AJOUTÉES ICI

    /**
     * Envoie un message au serveur
     */
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                System.out.println("Message envoyé au serveur : " + message);
            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi du message : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Impossible d'envoyer le message, session inactive.");
            messageHandlers.forEach(handler -> handler.accept("SYSTEM_MSG:Not_Connected_Cannot_Send"));
        }
    }

    /**
     * Vérifie si la connexion est active
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Ajoute un gestionnaire de messages
     */
    public void addMessageHandler(Consumer<String> handler) {
        this.messageHandlers.add(handler);
    }

    /**
     * Supprime un gestionnaire de messages
     */
    public void removeMessageHandler(Consumer<String> handler) {
        this.messageHandlers.remove(handler);
    }

    /**
     * Ferme la connexion WebSocket
     */
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