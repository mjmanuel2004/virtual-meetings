package com.datingapp.client.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;
    private final boolean sentByCurrentUser;
    private final MessageType type;
    private final String senderAvatarUrl; // New field

    public enum MessageType {
        GENERAL, DIRECT, SYSTEM_NOTIFICATION, USER_EVENT 
    }

    // Constructor for live messages (timestamp generated now)
    public ChatMessage(String sender, String content, String senderAvatarUrl, boolean sentByCurrentUser, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now(); // Timestamp generated on client side for new messages
        this.sentByCurrentUser = sentByCurrentUser;
        this.type = type;
        this.senderAvatarUrl = (senderAvatarUrl == null || senderAvatarUrl.trim().isEmpty()) ? null : senderAvatarUrl.trim();
    }
    
    // Constructor for messages from history (timestamp provided)
    public ChatMessage(String sender, String content, LocalDateTime timestamp, boolean sentByCurrentUser, MessageType type, String senderAvatarUrl) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.sentByCurrentUser = sentByCurrentUser;
        this.type = type;
        this.senderAvatarUrl = (senderAvatarUrl == null || senderAvatarUrl.trim().isEmpty()) ? null : senderAvatarUrl.trim();
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSentByCurrentUser() { return sentByCurrentUser; }
    public MessageType getType() { return type; }
    public String getSenderAvatarUrl() { return senderAvatarUrl; } // Getter for avatar URL

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public String toString() {
        return (sentByCurrentUser ? "Me" : sender) + ": " + content + " (" + getFormattedTimestamp() + ") Avatar: " + senderAvatarUrl;
    }
}
