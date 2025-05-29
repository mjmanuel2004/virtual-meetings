package com.datingapp.server;

// ... (all existing imports from previous versions)
import javax.websocket.OnClose; // etc.
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher; // For simple JSON parsing
import java.util.regex.Pattern; // For simple JSON parsing
import java.util.stream.Collectors;


@ServerEndpoint(value = "/chat/{clientUsername}")
public class ChatServerEndpoint {

    // ... (static fields: activeSessions, userSessions, sessionMeetingCodes, sessionUsernames, userAvatarUrls, MESSAGE_HISTORY_LIMIT - assumed present)
    private static Set<Session> activeSessions = Collections.synchronizedSet(new HashSet<>());
    private static Map<String, Session> userSessions = new ConcurrentHashMap<>();
    private static Map<Session, String> sessionMeetingCodes = new ConcurrentHashMap<>();
    private static Map<Session, String> sessionUsernames = new ConcurrentHashMap<>();
    private static Map<String, String> userAvatarUrls = new ConcurrentHashMap<>(); 
    private static Map<String, String> userBios = new ConcurrentHashMap<>(); // Cache for user bios

    private static final int MESSAGE_HISTORY_LIMIT = 50;
    // Updated pattern to be more robust for simple cases, but still basic.
    // Looks for "bio":" possibly_escaped_value "
    private static final Pattern JSON_BIO_PATTERN = Pattern.compile("\"bio\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");


    @OnOpen
    public void onOpen(Session session, @PathParam("clientUsername") String clientUsername) { 
        activeSessions.add(session); System.out.println("New connection: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Raw message from " + session.getId() + " ("+ sessionUsernames.get(session) +"): " + message);
        String currentUsername = sessionUsernames.get(session);

        if (message.startsWith("REGISTER:")) { handleRegistration(message, session); }
        else if (message.startsWith("LOGIN:")) { handleLogin(message, session); }
        else if (currentUsername == null) { sendMessage(session, "ERROR:Authentication required."); return; }
        else if (message.startsWith("MEETING_CODE:")) { handleMeetingCode(message, session, currentUsername); }
        else if (message.startsWith("DM_SEND:")) { handleDirectMessage(message, session, currentUsername); }
        else if (message.startsWith("REQ_DM_HIST:")) { handleRequestDmHistory(message, session, currentUsername); }
        else if (message.startsWith("REQ_MEETING_HIST:")) { handleRequestMeetingHistory(message, session, currentUsername); }
        else if (message.startsWith("UPDATE_AVATAR_URL:")) { handleUpdateAvatarUrl(message, session, currentUsername); }
        else if (message.startsWith("UPDATE_PROFILE:")) { handleUpdateProfile(message, session, currentUsername); } // New handler
        else { handleChatMessage(message, session, currentUsername); }
    }

    private void handleLogin(String message, Session session) {
        String[] parts = message.split(":", 3);
        if (parts.length == 3) {
            String username = parts[1];
            String password = parts[2];
            // Fetch avatar_url and bio
            String sql = "SELECT id, password_hash, avatar_url, bio FROM users WHERE username = ?";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPasswordHash = rs.getString("password_hash");
                    int userId = rs.getInt("id");
                    String avatarUrl = rs.getString("avatar_url");
                    String bio = rs.getString("bio");

                    if (avatarUrl == null) avatarUrl = "";
                    if (bio == null) bio = "";

                    if (PasswordUtil.checkPassword(password, storedPasswordHash)) {
                        // ... (handle old session as before) ...
                        Session oldSession = userSessions.get(username);
                        if (oldSession != null && oldSession.isOpen()) { sendMessage(oldSession, "SYSTEM_MSG:Logged in from another location."); try { oldSession.close(); } catch (IOException e) { e.printStackTrace(); } }
                        
                        userSessions.put(username, session);
                        sessionUsernames.put(session, username);
                        session.getUserProperties().put("userId", userId);
                        userAvatarUrls.put(username, avatarUrl);
                        userBios.put(username, bio); // Cache bio

                        // LOGIN_SUCCESS:Welcome username:avatar_url_or_empty:bio_or_empty
                        sendMessage(session, "LOGIN_SUCCESS:Welcome " + username + ":" + avatarUrl + ":" + bio);
                        System.out.println("User " + username + " (ID: " + userId + ", Avatar: " + avatarUrl + ", Bio: " + bio.substring(0, Math.min(bio.length(), 20)) + "...) logged in.");
                        
                        String meetingCode = sessionMeetingCodes.getOrDefault(session, "public");
                        sessionMeetingCodes.put(session, meetingCode);
                        broadcastUserStatus(username, meetingCode, true, session, false); 
                    } else sendMessage(session, "LOGIN_FAIL:Invalid credentials.");
                } else sendMessage(session, "LOGIN_FAIL:User not found.");
            } catch (SQLException e) { sendMessage(session, "LOGIN_FAIL:DB Error: " + e.getMessage()); e.printStackTrace(); }
        } else sendMessage(session, "LOGIN_FAIL:Invalid format.");
    }
    
    private String parseJsonBio(String jsonPayload) {
        Matcher matcher = JSON_BIO_PATTERN.matcher(jsonPayload);
        if (matcher.find()) {
            String bio = matcher.group(1);
            // Basic unescaping for \", \\, \n, \r, \t
            return bio.replace("\\\"", "\"")
                      .replace("\\\\", "\\")
                      .replace("\\n", "\n")
                      .replace("\\r", "")   // Typically remove \r
                      .replace("\\t", "\t");
        }
        System.err.println("Could not parse bio from JSON: " + jsonPayload);
        return null;
    }

    private void handleUpdateProfile(String message, Session session, String currentUsername) {
        // Format: UPDATE_PROFILE:{"bio":"new bio text"}
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String jsonPayload = parts[1];
            String newBio = parseJsonBio(jsonPayload); // Use the updated parser

            if (newBio == null) {
                 sendMessage(session, "PROFILE_UPDATE_FAIL:Invalid JSON payload format for bio.");
                 return;
            }
             if (newBio.length() > 2000) { // Example limit for TEXT column
                sendMessage(session, "PROFILE_UPDATE_FAIL:Bio is too long (max 2000 chars).");
                return;
            }

            Integer userId = (Integer) session.getUserProperties().get("userId");
            if (userId == null) { sendMessage(session, "PROFILE_UPDATE_FAIL:User ID not found."); return; }

            String sql = "UPDATE users SET bio = ? WHERE id = ?";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newBio);
                pstmt.setInt(2, userId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    userBios.put(currentUsername, newBio); // Update cache
                    // Construct the success payload to send back, ensuring bio is correctly escaped for JSON
                    String escapedBio = newBio.replace("\\", "\\\\").replace("\"", "\\\"");
                    String successPayload = "{\"bio\":\"" + escapedBio + "\"}";
                    sendMessage(session, "PROFILE_UPDATE_SUCCESS:" + successPayload); 
                    broadcastUserStatus(currentUsername, sessionMeetingCodes.getOrDefault(session, "public"), true, session, true); // true for isProfileUpdate
                } else {
                    sendMessage(session, "PROFILE_UPDATE_FAIL:Could not update bio in DB.");
                }
            } catch (SQLException e) {
                sendMessage(session, "PROFILE_UPDATE_FAIL:DB error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            sendMessage(session, "PROFILE_UPDATE_FAIL:Invalid message format.");
        }
    }

    private void broadcastUserStatus(String username, String meetingCode, boolean joined, Session sourceSession, boolean isProfileUpdate) {
        String avatarUrl = userAvatarUrls.getOrDefault(username, "");
        String bio = userBios.getOrDefault(username, ""); 
        String statusMessage;

        if (isProfileUpdate) { 
             // Ensure bio is escaped for JSON-like structure if it contains special characters
             String escapedBio = bio.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
             statusMessage = "USER_PROFILE_UPDATE:" + username + ":" + avatarUrl + ":" + escapedBio;
        } else { 
             if (joined) statusMessage = "USER_JOINED:" + username + ":" + avatarUrl;
             else statusMessage = "USER_LEFT:" + username; 
        }

        String effectiveCode = (meetingCode == null || meetingCode.trim().isEmpty()) ? "public" : meetingCode.trim();
        System.out.println("Broadcasting status: " + statusMessage + " to code: " + effectiveCode);

        for (Session s : activeSessions) {
            if (s.isOpen() && (isProfileUpdate || !s.equals(sourceSession))) { 
                String targetSessionMeetingCode = sessionMeetingCodes.getOrDefault(s, "public");
                
                // Simplified broadcast logic:
                // If event is for "public", only "public" users get it.
                // If event is for a specific code, only users in that code get it.
                // If it's a profile update, this rule still applies.
                if (targetSessionMeetingCode.equals(effectiveCode)) {
                    sendMessage(s, statusMessage);
                }
            }
        }
    }
    
    private void handleUpdateAvatarUrl(String message, Session session, String currentUsername) {
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String newAvatarUrl = parts[1]; Integer userId = (Integer) session.getUserProperties().get("userId");
            if (userId == null) { sendMessage(session, "AVATAR_UPDATE_FAIL:User ID not found."); return; }
            if (newAvatarUrl.length() > 512 || (!newAvatarUrl.isEmpty() && !newAvatarUrl.matches("^https?://.*"))) {
                 sendMessage(session, "AVATAR_UPDATE_FAIL:Invalid URL."); return;
            }
            String sql = "UPDATE users SET avatar_url = ? WHERE id = ?";
            try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newAvatarUrl.isEmpty() ? null : newAvatarUrl); // Store NULL if empty
                pstmt.setInt(2, userId);
                if (pstmt.executeUpdate() > 0) {
                    userAvatarUrls.put(currentUsername, newAvatarUrl.isEmpty() ? "" : newAvatarUrl); 
                    sendMessage(session, "AVATAR_UPDATE_SUCCESS:" + newAvatarUrl);
                    broadcastUserStatus(currentUsername, sessionMeetingCodes.getOrDefault(session, "public"), true, session, true); 
                } else sendMessage(session, "AVATAR_UPDATE_FAIL:DB update failed.");
            } catch (SQLException e) { sendMessage(session, "AVATAR_UPDATE_FAIL:DB error: " + e.getMessage()); e.printStackTrace(); }
        } else sendMessage(session, "AVATAR_UPDATE_FAIL:Invalid format.");
    }

    private String mapListToJsonArray(List<Map<String, Object>> list) {
        return "[" + list.stream()
            .map(messageMap -> "{" +
                messageMap.entrySet().stream()
                    .map(entry -> "\"" + entry.getKey().replace("\"", "\\\"") + "\":\"" +
                                 (entry.getValue() instanceof LocalDateTime ? 
                                  ((LocalDateTime)entry.getValue()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : 
                                  entry.getValue().toString().replace("\"", "\\\"").replace("\n", "\\n")) + // Escape newlines too
                                 "\"")
                    .collect(Collectors.joining(",")) +
                "}")
            .collect(Collectors.joining(",")) + "]";
    }

    // Stubs for other methods (ensure they are complete in your actual file)
    private void handleRequestDmHistory(String m, Session s, String cu) { 
        String[] parts = m.split(":", 2); if (parts.length == 2) { String otherUsername = parts[1]; Integer cuid = (Integer) s.getUserProperties().get("userId"); Integer ouid = getUserIdByUsername(otherUsername); if (cuid!=null && ouid!=null) { List<Map<String,Object>> h = DatabaseUtil.getDmHistory(cuid,ouid,MESSAGE_HISTORY_LIMIT); sendMessage(s, "RESP_DM_HIST:"+otherUsername+":"+mapListToJsonArray(h)); } else {sendMessage(s, "ERROR:User not found for DM history");}} else {sendMessage(s, "ERROR:Invalid REQ_DM_HIST");}
    }
    private void handleRequestMeetingHistory(String m, Session s, String cu) { 
        String[] parts = m.split(":", 2); if (parts.length == 2) { String mc = parts[1]; if(mc.trim().isEmpty()){sendMessage(s,"ERROR:Meeting code empty"); return;} List<Map<String,Object>> h = DatabaseUtil.getMeetingCodeHistory(mc,MESSAGE_HISTORY_LIMIT); sendMessage(s, "RESP_MEETING_HIST:"+mc+":"+mapListToJsonArray(h));} else {sendMessage(s, "ERROR:Invalid REQ_MEETING_HIST");}
    }
    private void handleRegistration(String message, Session session) { 
        String[] parts = message.split(":", 4);
        if (parts.length == 4) {
            String username = parts[1]; String password = parts[2]; String email = parts[3];
            String checkUserSql = "SELECT id FROM users WHERE username = ? OR email = ?";
            try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
                checkStmt.setString(1, username); checkStmt.setString(2, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) { sendMessage(session, "REGISTER_FAIL:Username or email already exists."); return; }
            } catch (SQLException e) { sendMessage(session, "REGISTER_FAIL:DB error: " + e.getMessage()); return; }
            String hashedPassword = PasswordUtil.hashPassword(password);
            // Avatar URL and bio will be NULL by default from DB schema
            String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)"; 
            try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username); pstmt.setString(2, hashedPassword); pstmt.setString(3, email);
                if (pstmt.executeUpdate() > 0) sendMessage(session, "REGISTER_SUCCESS:Registration successful. Please login.");
                else sendMessage(session, "REGISTER_FAIL:Registration failed.");
            } catch (SQLException e) { sendMessage(session, "REGISTER_FAIL:Error: " + e.getMessage()); e.printStackTrace();}
        } else sendMessage(session, "REGISTER_FAIL:Invalid format.");
    }
    private void handleMeetingCode(String message, Session session, String currentUsername) { 
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String oldCode = sessionMeetingCodes.getOrDefault(session, "public");
            String newCode = parts[1].trim();
            if (newCode.isEmpty()) newCode = "public";
            sessionMeetingCodes.put(session, newCode);
            sendMessage(session, "MEETING_CODE_STATUS:Joined code: " + newCode);
            if (!oldCode.equals(newCode)) {
                broadcastUserStatus(currentUsername, oldCode, false, session, false); 
                broadcastUserStatus(currentUsername, newCode, true, session, false);  
            }
        } else sendMessage(session, "ERROR:Invalid meeting code format.");
    }
    private Integer getUserIdByUsername(String username) { 
        String sql = "SELECT id FROM users WHERE username = ?"; try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { pstmt.setString(1, username); ResultSet rs = pstmt.executeQuery(); if (rs.next()) { return rs.getInt("id"); } } catch (SQLException e) { e.printStackTrace(); } return null; 
    }
    private void handleDirectMessage(String message, Session senderSession, String senderUsername) { 
        String[] parts = message.split(":", 3); if (parts.length == 3) { String ru = parts[1]; String mc = parts[2]; Integer sid = (Integer) senderSession.getUserProperties().get("userId"); if(sid==null){sendMessage(senderSession,"ERROR:SID Null");return;} if(ru.equals(senderUsername)){sendMessage(senderSession,"ERROR:DM Self");return;} Integer rid = getUserIdByUsername(ru); if(rid==null){sendMessage(senderSession,"ERROR:RID Null");return;} String sql = "INSERT INTO messages (sender_id, receiver_id, content, meeting_code) VALUES (?, ?, ?, ?)"; try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { pstmt.setInt(1, sid); pstmt.setInt(2, rid); pstmt.setString(3, mc); pstmt.setString(4, "_DM_"); pstmt.executeUpdate(); sendMessage(senderSession, "DM_SENT_CONFIRM:" + ru + ":" + mc); Session rs = userSessions.get(ru); if (rs != null && rs.isOpen()) { sendMessage(rs, "DM_RECEIVE:" + senderUsername + ":" + mc); } else { sendMessage(senderSession, "SYSTEM_MSG:User " + ru + " is offline."); } } catch (SQLException e) { e.printStackTrace(); sendMessage(senderSession, "ERROR:DM Fail"); } } else sendMessage(senderSession, "ERROR:Invalid DM format.");
    }
    private void handleChatMessage(String messageContent, Session session, String senderUsername) { 
        Integer sid = (Integer) session.getUserProperties().get("userId"); if(sid==null){sendMessage(session,"ERROR:UID Null");return;} String mc = sessionMeetingCodes.getOrDefault(session, "public"); String sql = "INSERT INTO messages (sender_id, content, meeting_code) VALUES (?, ?, ?)"; try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { pstmt.setInt(1, sid); pstmt.setString(2, messageContent); pstmt.setString(3, mc); pstmt.executeUpdate(); } catch (SQLException e) { e.printStackTrace(); } String fm = "MSG:" + senderUsername + ":" + messageContent; broadcastMessage(fm, mc, null);
    }
    @OnClose public void onClose(Session session) { 
        activeSessions.remove(session);
        String username = sessionUsernames.remove(session);
        String meetingCode = sessionMeetingCodes.remove(session);
        if (username != null) {
            userSessions.remove(username); 
            userAvatarUrls.remove(username); // Remove avatar from cache
            userBios.remove(username);       // Remove bio from cache
            System.out.println("User " + username + " disconnected. Session: " + session.getId());
            broadcastUserStatus(username, meetingCode, false, session, false); 
        } else {
            System.out.println("Session " + session.getId() + " disconnected (was not fully authenticated).");
        }
    }
    @OnError public void onError(Session s, Throwable t) { 
        System.err.println("Error on session " + s.getId() + ": " + t.getMessage());
        t.printStackTrace(); 
    }
    private void broadcastMessage(String message, String meetingCode, Session senderSession) { 
        String ec = (meetingCode==null||meetingCode.trim().isEmpty())?"public":meetingCode.trim(); for(Session s:activeSessions){if(s.isOpen()){String tsc = sessionMeetingCodes.getOrDefault(s,"public"); if(tsc.equals(ec)){sendMessage(s,message);}}}
    }
    private void sendMessage(Session s, String m) { 
        if(s!=null && s.isOpen()){try{s.getBasicRemote().sendText(m);}catch(IOException e){e.printStackTrace();}}
    }
}
