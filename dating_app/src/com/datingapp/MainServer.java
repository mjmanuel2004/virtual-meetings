package com.datingapp;

import com.datingapp.server.ChatServerEndpoint; // Assuming this is the correct package
import org.glassfish.tyrus.server.Server; // Assuming Tyrus JARs are in lib/

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MainServer {

    private static final Properties serverProperties = new Properties();
    private static final String PROPERTIES_FILE = "server.properties"; // Relative path to file

    static {
        // Try to load from classpath first (e.g., if running from JAR)
        try (InputStream input = MainServer.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                serverProperties.load(input);
                System.out.println("Loaded server properties from classpath: " + PROPERTIES_FILE);
            } else {
                // Fallback to file system (e.g., during development)
                try (InputStream fsInput = new FileInputStream(PROPERTIES_FILE)) {
                    serverProperties.load(fsInput);
                    System.out.println("Loaded server properties from file system: " + PROPERTIES_FILE);
                } catch (IOException ex) {
                    System.err.println("Warning: Could not load " + PROPERTIES_FILE + 
                                       " from classpath or file system. Using default values.");
                    // ex.printStackTrace(); // Optionally print stack trace
                }
            }
        } catch (IOException ex) {
            System.err.println("Warning: Error attempting to load " + PROPERTIES_FILE + 
                               ". Using default values.");
            // ex.printStackTrace();
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
            System.err.println("Warning: Could not parse integer for property '" + key + "'. Using default value " + defaultValue);
        }
        return defaultValue;
    }

    public static void main(String[] args) {
        System.out.println("Server Application Started");

        // DatabaseUtil will load its own properties as per its static block.
        // If we wanted MainServer to pass properties to DatabaseUtil, we'd do:
        // com.datingapp.server.DatabaseUtil.initializeWithProperties(serverProperties);

        int serverPort = getIntProperty("server.port", 8025); // Default if not in properties
        
        Server server = new Server("localhost", serverPort, "/websockets", null, ChatServerEndpoint.class);
        try {
            server.start();
            System.out.println("WebSocket server started on ws://localhost:" + serverPort + "/websockets/chat");
            // Example of getting other properties, though DatabaseUtil now loads its own
            System.out.println("DB URL from MainServer (for reference): " + getProperty("db.url", "jdbc:mysql://localhost:3306/dating_app_db_default"));
            
            System.out.println("Press any key to stop the server...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
            System.out.println("Server stopped.");
        }
    }
}
