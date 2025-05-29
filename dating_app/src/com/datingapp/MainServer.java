package com.datingapp;

import com.datingapp.server.ChatServerEndpoint;
import org.glassfish.tyrus.server.Server;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

public class MainServer {

    private static final Properties serverProperties = new Properties();
    private static final String PROPERTIES_FILE = "server.properties";

    static {
        // Chargement des propriétés (le code existant reste identique)
        try (InputStream input = MainServer.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                serverProperties.load(input);
                System.out.println("Propriétés du serveur chargées depuis le classpath : " + PROPERTIES_FILE);
            } else {
                try (InputStream fsInput = new FileInputStream(PROPERTIES_FILE)) {
                    serverProperties.load(fsInput);
                    System.out.println("Propriétés du serveur chargées depuis le système de fichiers : " + PROPERTIES_FILE);
                } catch (IOException ex) {
                    System.err.println("Attention : Impossible de charger " + PROPERTIES_FILE +
                            " depuis le classpath ou le système de fichiers. Utilisation des valeurs par défaut.");
                }
            }
        } catch (IOException ex) {
            System.err.println("Attention : Erreur lors de la tentative de chargement de " + PROPERTIES_FILE +
                    ". Utilisation des valeurs par défaut.");
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return serverProperties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key, int defaultValue) {
        try {
            String value = serverProperties.getProperty(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            System.err.println("Attention : Impossible d'analyser l'entier pour la propriété '" + key + "'. Utilisation de la valeur par défaut " + defaultValue);
        }
        return defaultValue;
    }

    // Méthode pour vérifier si le port est disponible
    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("Application Serveur Démarrée");

        int serverPort = getIntProperty("server.port", 8025);

        // Vérifier si le port est disponible
        if (!isPortAvailable(serverPort)) {
            System.err.println("ERREUR : Le port " + serverPort + " est déjà utilisé !");
            System.err.println("Veuillez soit :");
            System.err.println("1. Arrêter le processus utilisant le port " + serverPort);
            System.err.println("2. Changer le server.port dans server.properties");
            System.err.println("3. Utiliser un port différent (essayez 8026, 8027, etc.)");
            return;
        }

        System.out.println("Le port " + serverPort + " est disponible, procédure de démarrage du serveur...");

        // Tester la connexion à la base de données avant de démarrer le serveur
        try {
            System.out.println("Test de la connexion à la base de données...");
            com.datingapp.server.DatabaseUtil.getConnection().close();
            System.out.println("Connexion à la base de données réussie.");
        } catch (Exception e) {
            System.err.println("ERREUR : Échec de la connexion à la base de données !");
            System.err.println("URL DB : " + getProperty("db.url", "jdbc:mysql://localhost:3306/dating_app_db_default"));
            System.err.println("Erreur : " + e.getMessage());
            System.err.println("Veuillez vous assurer que :");
            System.err.println("1. Le serveur MySQL fonctionne");
            System.err.println("2. La base de données existe");
            System.err.println("3. Les identifiants dans server.properties sont corrects");
            return;
        }

        Server server = null;
        try {
            // Créer l'instance du serveur
            System.out.println("Création du serveur WebSocket...");
            server = new Server("localhost", serverPort, "/websockets", null, ChatServerEndpoint.class);

            // Démarrer le serveur avec une gestion d'exceptions appropriée
            System.out.println("Démarrage du serveur WebSocket sur ws://localhost:" + serverPort + "/websockets/chat");
            server.start();

            System.out.println("✓ Serveur WebSocket démarré avec succès !");
            System.out.println("Endpoint du serveur : ws://localhost:" + serverPort + "/websockets/chat/{username}");
            System.out.println("URL DB : " + getProperty("db.url", "jdbc:mysql://localhost:3306/dating_app_db_default"));

            System.out.println("Appuyez sur Entrée pour arrêter le serveur...");
            System.in.read();

        } catch (IOException e) {
            System.err.println("ERREUR : Erreur I/O pendant le fonctionnement du serveur !");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            System.err.println("ERREUR : Erreur pendant le démarrage du serveur !");

            // Analyser le type d'exception pour donner des conseils spécifiques
            String errorMsg = e.getMessage();
            String exceptionType = e.getClass().getSimpleName();

            System.err.println("Type d'exception : " + exceptionType);
            System.err.println("Message : " + errorMsg);

            if (exceptionType.contains("Deployment") || errorMsg.contains("deployment")) {
                System.err.println("Cela indique généralement :");
                System.err.println("1. Problème avec l'annotation @ServerEndpoint");
                System.err.println("2. Problème de connexion à la base de données pendant l'initialisation de l'endpoint");
                System.err.println("3. Dépendances manquantes");
            } else if (errorMsg.contains("bind") || errorMsg.contains("port") || errorMsg.contains("address")) {
                System.err.println("Problème de liaison de port - le port pourrait être déjà utilisé");
            } else if (errorMsg.contains("database") || errorMsg.contains("connection") || errorMsg.contains("sql")) {
                System.err.println("Problème de base de données - vérifiez la connexion MySQL");
            }

            e.printStackTrace();
            return;
        } finally {
            // Arrêt sécurisé du serveur
            if (server != null) {
                try {
                    System.out.println("Arrêt du serveur...");
                    server.stop();
                    System.out.println("Serveur arrêté avec succès.");
                } catch (Exception e) {
                    System.err.println("Erreur pendant l'arrêt du serveur : " + e.getMessage());
                }
            }
        }
    }
}