package com.datingapp.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Import Properties and FileInputStream if loading directly here,
// or expect properties to be passed from MainServer/ConfigUtil.
// For this example, we'll assume MainServer calls setters or a static init method.

public class DatabaseUtil {
    // These will now be set from properties
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;
    private static boolean propertiesLoaded = false;

    // Static initializer to load properties.
    // This makes DatabaseUtil self-sufficient in loading its config if MainServer doesn't pass them.
    // Alternatively, MainServer could call specific setters.
    static {
        // Rely on MainServer to load properties and call setters, or implement loading here too.
        // For simplicity, let's assume MainServer will handle loading and can call setters if needed,
        // or we use a shared properties object.
        // Let's use the getProperty from MainServer for now, though this creates a dependency.
        // A cleaner way would be a shared Config class or dependency injection.

        // To make it runnable for the subtask, let's use MainServer's static methods.
        // This is not ideal for separation of concerns but works for this structure.
        // In a real app, a dedicated Properties singleton/service would be better.
        try {
             // This approach creates a direct dependency from DatabaseUtil to MainServer's static methods.
             // Consider passing Properties object to an init method in DatabaseUtil instead.
             // For now, to keep it simple and avoid changing MainServer to pass props:
             java.util.Properties props = new java.util.Properties();
             try (java.io.InputStream input = new java.io.FileInputStream("server.properties")) {
                 props.load(input);
                 DB_URL = props.getProperty("db.url", "jdbc:mysql://localhost:3306/dating_app_db_default");
                 DB_USER = props.getProperty("db.user", "dating_app_user_default");
                 DB_PASSWORD = props.getProperty("db.password", "userpassword_default");
                 propertiesLoaded = true;
                 System.out.println("DatabaseUtil loaded configuration from server.properties");
             } catch (java.io.IOException ex) {
                 System.err.println("DatabaseUtil: Could not load server.properties. Using hardcoded defaults or expecting them to be set.");
                 // Fallback to some defaults if file not found
                 DB_URL = "jdbc:mysql://localhost:3306/dating_app_db_fallback";
                 DB_USER = "dating_app_user_fallback";
                 DB_PASSWORD = "userpassword_fallback";
                 propertiesLoaded = false; // Explicitly set to false on IO error
             }

        } catch (Exception e) {
            System.err.println("Error loading database configuration: " + e.getMessage());
            // Set hardcoded defaults if properties fail for any reason
             DB_URL = "jdbc:mysql://localhost:3306/dating_app_db_hardcoded";
             DB_USER = "dating_app_user_hardcoded";
             DB_PASSWORD = "userpassword_hardcoded";
             propertiesLoaded = false; // Explicitly set to false on other errors
        }


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Add mysql-connector-java.jar to your classpath.");
            // This is a critical error, application might not function.
        }
    }
    
    // Constructor could be private if all methods are static and it's purely a utility class
    public DatabaseUtil() {}


    public static Connection getConnection() throws SQLException {
        if (!propertiesLoaded && (DB_URL == null || DB_URL.contains("_fallback") || DB_URL.contains("_hardcoded") || DB_URL.contains("_placeholder"))) { 
            System.err.println("Database configuration not properly loaded. Attempting with critical placeholder defaults.");
            // Attempt placeholder defaults if initialization failed badly
            DB_URL = "jdbc:mysql://localhost:3306/dating_app_db_placeholder"; 
            DB_USER = "user_placeholder";
            DB_PASSWORD = "password_placeholder";
             // This situation indicates a severe configuration problem.
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // close method (remains unchanged)
    public static void close(AutoCloseable... resources) { 
        for (AutoCloseable resource : resources) {
            if (resource != null) { try { resource.close(); } catch (Exception e) { e.printStackTrace(); } }
        }
    }

    // getDmHistory method (remains unchanged)
    public static List<Map<String, Object>> getDmHistory(int userId1, int userId2, int limit) { 
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT m.content, m.timestamp, u.username AS sender_username " +
                     "FROM (SELECT * FROM messages " +
                     "      WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                     "            AND meeting_code = '_DM_' " + 
                     "      ORDER BY timestamp DESC LIMIT ?) AS m " + 
                     "JOIN users u ON m.sender_id = u.id " +
                     "ORDER BY m.timestamp ASC"; 
        Connection conn = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            conn = getConnection(); pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId1); pstmt.setInt(2, userId2); pstmt.setInt(3, userId2); pstmt.setInt(4, userId1); pstmt.setInt(5, limit);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> message = new HashMap<>();
                message.put("sender_username", rs.getString("sender_username"));
                message.put("content", rs.getString("content"));
                message.put("timestamp", rs.getTimestamp("timestamp").toLocalDateTime());
                history.add(message);
            }
        } catch (SQLException e) { e.printStackTrace(); } finally { close(rs, pstmt, conn); }
        return history;
    }

    // getMeetingCodeHistory method (remains unchanged)
    public static List<Map<String, Object>> getMeetingCodeHistory(String meetingCode, int limit) { 
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT m.content, m.timestamp, u.username AS sender_username " +
                     "FROM (SELECT * FROM messages " +
                     "      WHERE meeting_code = ? AND receiver_id IS NULL " + 
                     "      ORDER BY timestamp DESC LIMIT ?) AS m " +
                     "JOIN users u ON m.sender_id = u.id " +
                     "ORDER BY m.timestamp ASC";
        Connection conn = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            conn = getConnection(); pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, meetingCode); pstmt.setInt(2, limit);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> message = new HashMap<>();
                message.put("sender_username", rs.getString("sender_username"));
                message.put("content", rs.getString("content"));
                message.put("timestamp", rs.getTimestamp("timestamp").toLocalDateTime());
                history.add(message);
            }
        } catch (SQLException e) { e.printStackTrace(); } finally { close(rs, pstmt, conn); }
        return history;
    }
    
    // Remove setDbUrl, setDbUser, setDbPassword if loading directly in static block
}
