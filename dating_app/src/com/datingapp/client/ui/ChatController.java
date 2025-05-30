package com.datingapp.client.ui;

import com.datingapp.MainClient;
import com.datingapp.client.model.ChatMessage;
import com.datingapp.client.model.UserProfile;
import com.datingapp.client.services.WebSocketClientService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow; // Added for emoji support
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField meetingCodeField;
    @FXML
    private Button joinMeetingButton;
    @FXML
    private Label meetingStatusLabel;
    @FXML
    private ListView<ChatMessage> chatListView;
    @FXML
    private TextField messageInputField;
    @FXML
    private Button sendButton;
    @FXML
    private ListView<String> userListView;
    @FXML
    private Button profileButton;

    private MainClient mainClientApp;
    private WebSocketClientService webSocketService;
    private String currentUsername;
    private String currentUserAvatarUrl;
    private ObservableList<ChatMessage> chatMessages = FXCollections.observableArrayList();
    private ObservableList<String> activeUsernames = FXCollections.observableArrayList();
    private String currentDmPartner = null;

    private Map<String, UserProfile> userProfilesCache = new ConcurrentHashMap<>();
    private static final Pattern JSON_PROPERTY_PATTERN = Pattern.compile("\"([^\"]+)\":\"([^\"]*)\"");

    // Define EMOJI_MAP (Using Map.of for Java 9+)
    // For Java 8, use a static block with HashMap.put.
    private static final Map<String, String> EMOJI_MAP = Map.of(
            ":)", "☺",        // U+263A White Smiling Face
            ":(", "☹",        // U+2639 White Frowning Face
            "<3", "❤",        // U+2764 Heavy Black Heart
            ":D", "😃",        // U+1F603 Smiling Face With Open Mouth
            ";)", "😉",        // U+1F609 Winking Face
            ":P", "😛",        // U+1F61B Face With Stuck-Out Tongue
            ":O", "😮",        // U+1F62E Face With Open Mouth
            ":'(", "😢",       // U+1F622 Crying Face
            "xD", "😂"         // U+1F602 Face With Tears of Joy (approximation for xD)
    );

    // Helper method to replace shortcodes with emojis
    private String replaceEmojiShortcodes(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }


    public void initialize() {
        chatListView.setItems(chatMessages);
        userListView.setItems(activeUsernames);
        chatListView.setCellFactory(listView -> new ChatMessageCell());
        userListView.setCellFactory(listView -> new UserListCell());

        userListView.setOnMouseClicked(event -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null && !selectedUser.equals(currentUsername)) {
                switchToDmWithUser(selectedUser);
            }
        });
    }

    @FXML
    private void handleProfileButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/ProfileView.fxml"));
            VBox page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Profile");
            if (mainClientApp != null && mainClientApp.getPrimaryStage() != null) {
                dialogStage.initOwner(mainClientApp.getPrimaryStage());
                dialogStage.initModality(Modality.WINDOW_MODAL);
            } else {
                dialogStage.initModality(Modality.APPLICATION_MODAL);
            }
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            ProfileController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setWebSocketService(this.webSocketService);

            UserProfile currentProfile = userProfilesCache.computeIfAbsent(currentUsername,
                    k -> new UserProfile(k, currentUserAvatarUrl, ""));
            currentProfile.setAvatarUrl(currentUserAvatarUrl);
            controller.setUserProfile(currentProfile);

            dialogStage.setOnCloseRequest(e -> controller.cleanup());
            dialogStage.showAndWait();

            if (controller.isProfileDataChanged()) {
                UserProfile updatedProfileFromDialog = controller.getUpdatedUserProfile();
                if (updatedProfileFromDialog != null) {
                    userProfilesCache.put(currentUsername, updatedProfileFromDialog);
                    this.currentUserAvatarUrl = updatedProfileFromDialog.getAvatarUrl();
                    setCurrentUserAvatarUrl(this.currentUserAvatarUrl);
                    chatListView.refresh();
                    userListView.refresh();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open profile editor");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void setUsername(String username) {
        this.currentUsername = username;
    }

    public void setCurrentUserAvatarUrl(String avatarUrl) {
        this.currentUserAvatarUrl = (avatarUrl == null || avatarUrl.isEmpty()) ? null : avatarUrl.trim();
        userProfilesCache.computeIfAbsent(currentUsername, k -> new UserProfile(k, null, null)).setAvatarUrl(this.currentUserAvatarUrl);
        if (currentDmPartner == null) {
            welcomeLabel.setText("Welcome, " + currentUsername + (this.currentUserAvatarUrl == null ? "" : " (Avatar set)"));
        }
    }

    // 4. REMPLACER la méthode handleServerMessage (version améliorée)
    private void handleServerMessage(String rawMessage) {
        Platform.runLater(() -> {
            System.out.println("Processing raw server message: " + rawMessage);
            ChatMessage chatMsg = null;
            List<ChatMessage> historyMessages = new ArrayList<>();
            String[] parts;

            if (rawMessage.startsWith("LOGIN_SUCCESS:")) {
                parts = rawMessage.split(":", 4);
                if (parts.length == 4) {
                    String welcomeMsgAndUser = parts[1];
                    String username = welcomeMsgAndUser.substring("Welcome ".length()).trim();
                    String avatarUrl = parts[2];
                    String bio = parts[3];
                    setUsername(username);
                    setCurrentUserAvatarUrl(avatarUrl);
                    userProfilesCache.computeIfAbsent(username, k -> new UserProfile(k, avatarUrl, null)).setBio(bio);
                    meetingStatusLabel.getStyleClass().setAll("label", "status-label-success");
                    meetingStatusLabel.setText("Logged in as " + currentUsername);

                    // CORRECTION: Initialiser le contexte en "public"
                    updateUserListContext("public");
                }
            }
            else if (rawMessage.startsWith("CLEAR_USER_LIST:")) {
                // NOUVEAU: Message pour vider la liste des utilisateurs
                System.out.println("Clearing user list");
                activeUsernames.clear();
            }
            else if (rawMessage.startsWith("USER_JOINED:")) {
                parts = rawMessage.split(":", 3);
                if (parts.length >= 2) {
                    String userJoined = parts[1];
                    String avatarUrl = parts.length > 2 ? parts[2] : "";
                    userProfilesCache.put(userJoined, new UserProfile(userJoined, avatarUrl, ""));

                    // CORRECTION: Ajouter à la liste seulement si pas en mode DM et pas déjà présent
                    if (currentDmPartner == null && !userJoined.equals(currentUsername)) {
                        if (!activeUsernames.contains(userJoined)) {
                            activeUsernames.add(userJoined);
                            System.out.println("Added " + userJoined + " to active users list");
                        }
                        // Message de système seulement pour les nouveaux arrivants (pas pour le refresh)
                        if (!rawMessage.contains("REFRESH")) {
                            chatMsg = new ChatMessage("System", userJoined + " has joined.", null, false, ChatMessage.MessageType.USER_EVENT);
                        }
                    }
                }
            }
            else if (rawMessage.startsWith("USER_LEFT:")) {
                String userLeft = rawMessage.substring("USER_LEFT:".length());
                activeUsernames.remove(userLeft);
                userProfilesCache.remove(userLeft);

                // Message seulement si pas en mode DM
                if (currentDmPartner == null) {
                    chatMsg = new ChatMessage("System", userLeft + " has left.", null, false, ChatMessage.MessageType.USER_EVENT);
                }
                System.out.println("Removed " + userLeft + " from active users list");
            }
            else if (rawMessage.startsWith("USER_PROFILE_UPDATE:")) {
                parts = rawMessage.split(":", 4);
                if (parts.length == 4) {
                    String updatedUser = parts[1];
                    String newAvatarUrl = parts[2];
                    String newBio = parts[3].replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                    UserProfile profile = userProfilesCache.computeIfAbsent(updatedUser, k -> new UserProfile(k, null, null));
                    profile.setAvatarUrl(newAvatarUrl);
                    profile.setBio(newBio);
                    if (updatedUser.equals(currentUsername)) {
                        setCurrentUserAvatarUrl(newAvatarUrl);
                    }
                    chatListView.refresh();
                    userListView.refresh();
                    chatMsg = new ChatMessage("System", updatedUser + "'s profile updated.", null, false, ChatMessage.MessageType.SYSTEM_NOTIFICATION, null);
                }
            }
            else if (rawMessage.startsWith("RESP_DM_HIST:")) {
                parts = rawMessage.split(":", 3);
                if (parts.length == 3) {
                    String otherUser = parts[1]; String jsonArrayStr = parts[2];
                    if (currentDmPartner != null && currentDmPartner.equals(otherUser)) {
                        List<Map<String, String>> parsedMessages = parseSimpleJsonArray(jsonArrayStr);
                        for (Map<String, String> msgMap : parsedMessages) {
                            try {
                                LocalDateTime ts = LocalDateTime.parse(msgMap.get("timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                String senderUsername = msgMap.get("sender_username");
                                UserProfile senderProfile = userProfilesCache.get(senderUsername);
                                String senderAvatar = (senderProfile != null) ? senderProfile.getAvatarUrl() : null;
                                historyMessages.add(new ChatMessage(senderUsername, msgMap.get("content"), ts, senderUsername.equals(currentUsername), ChatMessage.MessageType.DIRECT, senderAvatar));
                            } catch (Exception e) { System.err.println("Error parsing DM history message: " + e); }
                        }
                    }
                }
            }
            else if (rawMessage.startsWith("RESP_MEETING_HIST:")) {
                parts = rawMessage.split(":", 3);
                if (parts.length == 3) {
                    String meetingCodeFromServer = parts[1]; String jsonArrayStr = parts[2];
                    String currentClientMeetingCode = meetingCodeField.getText().trim().isEmpty() ? "public" : meetingCodeField.getText().trim();
                    if (currentDmPartner == null && meetingCodeFromServer.equals(currentClientMeetingCode)) {
                        List<Map<String, String>> parsedMessages = parseSimpleJsonArray(jsonArrayStr);
                        for (Map<String, String> msgMap : parsedMessages) {
                            try {
                                LocalDateTime ts = LocalDateTime.parse(msgMap.get("timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                String senderUsername = msgMap.get("sender_username");
                                UserProfile senderProfile = userProfilesCache.get(senderUsername);
                                String senderAvatar = (senderProfile != null) ? senderProfile.getAvatarUrl() : null;
                                historyMessages.add(new ChatMessage(senderUsername, msgMap.get("content"), ts, senderUsername.equals(currentUsername), ChatMessage.MessageType.GENERAL, senderAvatar));
                            } catch (Exception e) { System.err.println("Error parsing Meeting history message: " + e); }
                        }
                    }
                }
            }
            else if (rawMessage.startsWith("MSG:")) {
                parts = rawMessage.split(":", 3);
                if (parts.length == 3) {
                    String sender = parts[1]; String content = parts[2];
                    UserProfile senderProfile = userProfilesCache.get(sender);
                    String senderAvatar = (senderProfile != null) ? senderProfile.getAvatarUrl() : null;
                    if (currentDmPartner == null) {
                        chatMsg = new ChatMessage(sender, content, null, sender.equals(currentUsername), ChatMessage.MessageType.GENERAL, senderAvatar);
                    }
                }
            }
            else if (rawMessage.startsWith("DM_RECEIVE:")) {
                parts = rawMessage.split(":", 3);
                if (parts.length == 3) {
                    String sender = parts[1]; String content = parts[2];
                    UserProfile senderProfile = userProfilesCache.get(sender);
                    String senderAvatar = (senderProfile != null) ? senderProfile.getAvatarUrl() : null;
                    if (currentDmPartner != null && currentDmPartner.equals(sender)) {
                        chatMsg = new ChatMessage(sender, content, null, false, ChatMessage.MessageType.DIRECT, senderAvatar);
                    }
                }
            }
            else if (rawMessage.startsWith("DM_SENT_CONFIRM:")) {
                parts = rawMessage.split(":", 3);
                if (parts.length == 3) {
                    String recipient = parts[1]; String content = parts[2];
                    if (currentDmPartner != null && currentDmPartner.equals(recipient)) {
                        chatMsg = new ChatMessage(currentUsername, content, null, true, ChatMessage.MessageType.DIRECT, currentUserAvatarUrl);
                    }
                }
            }
            else if (rawMessage.startsWith("MEETING_CODE_STATUS:")) {
                meetingStatusLabel.getStyleClass().setAll("label");
                meetingStatusLabel.setText(rawMessage.substring("MEETING_CODE_STATUS:".length()));
            }
            else if (rawMessage.startsWith("ERROR:")) {
                chatMsg = new ChatMessage("System", "Server Error: " + rawMessage.substring("ERROR:".length()), null, false, ChatMessage.MessageType.SYSTEM_NOTIFICATION);
                meetingStatusLabel.getStyleClass().setAll("label", "status-label-error");
                meetingStatusLabel.setText(rawMessage.substring("ERROR:".length()));
            }
            else if (rawMessage.startsWith("SYSTEM_MSG:")) {
                chatMsg = new ChatMessage("System", rawMessage.substring("SYSTEM_MSG:".length()), null, false, ChatMessage.MessageType.SYSTEM_NOTIFICATION);
            }
            else if (!rawMessage.startsWith("LOGIN_") && !rawMessage.startsWith("REGISTER_") && !rawMessage.startsWith("AVATAR_UPDATE_") && !rawMessage.startsWith("PROFILE_UPDATE_")) {
                parts = rawMessage.split(":", 2);
                if (parts.length == 2 && currentDmPartner == null) {
                    chatMsg = new ChatMessage(parts[0], parts[1],null, parts[0].equals(currentUsername), ChatMessage.MessageType.GENERAL, userProfilesCache.getOrDefault(parts[0], new UserProfile(parts[0],null, null)).getAvatarUrl());
                } else if (currentDmPartner == null) {
                    chatMsg = new ChatMessage("Server", rawMessage, null, false, ChatMessage.MessageType.GENERAL, null);
                }
            }

            if (!historyMessages.isEmpty()) {
                chatMessages.addAll(0, historyMessages);
                if (chatMessages.size() > historyMessages.size() && historyMessages.size() < chatListView.getItems().size()) {
                    chatListView.scrollTo(historyMessages.size() -1 );
                } else {
                    chatListView.scrollTo(0);
                }
            }
            if (chatMsg != null) {
                chatMessages.add(chatMsg);
                chatListView.scrollTo(chatMessages.size() - 1);
            }
        });
    }

    @FXML
    private void handleSendMessageAction(ActionEvent event) {
        String messageText = messageInputField.getText().trim();
        if (!messageText.isEmpty() && webSocketService != null && webSocketService.isConnected()) {
            if (currentDmPartner != null) {
                webSocketService.sendMessage("DM_SEND:" + currentDmPartner + ":" + messageText);
            } else {
                webSocketService.sendMessage(messageText);
            }
            messageInputField.clear();
        } else if (messageText.isEmpty()) { /* Optional: feedback */ } else {
            chatMessages.add(new ChatMessage("System", "Not connected to server.", null, false, ChatMessage.MessageType.SYSTEM_NOTIFICATION, null));
        }
    }

    private void switchToDmWithUser(String username) {
        currentDmPartner = username;
        meetingCodeField.setDisable(true);
        joinMeetingButton.setDisable(true);
        chatMessages.clear();
        meetingStatusLabel.setText("DM with " + username);

        // CORRECTION: Vider la liste et mettre à jour le contexte
        updateUserListContext("DM");

        if(webSocketService!=null && webSocketService.isConnected()){
            webSocketService.sendMessage("REQ_DM_HIST:"+currentDmPartner);
        } else {
            chatMessages.add(new ChatMessage("System","No connection for DM history",null,false,ChatMessage.MessageType.SYSTEM_NOTIFICATION,null));
        }
    }

    @FXML
    private void handleMeetingCodeAction(ActionEvent event) {
        String code = meetingCodeField.getText().trim();
        String effCode = code.isEmpty()?"public":code;

        if(webSocketService!=null && webSocketService.isConnected()){
            webSocketService.sendMessage("MEETING_CODE:"+effCode);
            currentDmPartner=null;
            meetingCodeField.setDisable(false);
            joinMeetingButton.setDisable(false);
            chatMessages.clear();

            // CORRECTION: Mettre à jour le contexte utilisateur
            updateUserListContext(effCode);

            webSocketService.sendMessage("REQ_MEETING_HIST:"+effCode);
        } else {
            meetingStatusLabel.getStyleClass().setAll("label", "status-label-error");
            meetingStatusLabel.setText("Not connected");
            chatMessages.add(new ChatMessage("System","Not connected for meeting code",null,false,ChatMessage.MessageType.SYSTEM_NOTIFICATION,null));
        }
    }

    public void setMainApp(MainClient mainApp) {
        this.mainClientApp = mainApp;
    }

    public void setWebSocketService(WebSocketClientService service) {
        this.webSocketService = service;
        this.webSocketService.addMessageHandler(this::handleServerMessage);
    }

    public void cleanUp() {
        if (webSocketService != null) {
            webSocketService.removeMessageHandler(this::handleServerMessage);
        }
    }

    private List<Map<String, String>> parseSimpleJsonArray(String jsonArrayStr) {
        List<Map<String, String>> list = new ArrayList<>();
        if (jsonArrayStr == null || !jsonArrayStr.startsWith("[") || !jsonArrayStr.endsWith("]")) {
            System.err.println("Invalid JSON: " + jsonArrayStr);
            return list;
        }
        if (jsonArrayStr.equals("[]")) return list;
        String content = jsonArrayStr.substring(1, jsonArrayStr.length() - 1).trim();
        if (content.isEmpty()) return list;
        String[] objectStrings = content.split("\\s*\\},\\s*\\{");
        for (String objStr : objectStrings) {
            Map<String, String> map = new HashMap<>();
            String currentObjStr = objStr.trim();
            if (!currentObjStr.startsWith("{")) currentObjStr = "{" + currentObjStr;
            if (!currentObjStr.endsWith("}")) currentObjStr = currentObjStr + "}";
            Matcher matcher = JSON_PROPERTY_PATTERN.matcher(currentObjStr);
            while (matcher.find()) {
                map.put(matcher.group(1), matcher.group(2).replace("\\\"", "\"").replace("\\\\", "\\"));
            }
            if (!map.isEmpty()) {
                list.add(map);
            }
        }
        return list;
    }

    // CORRECTION: Partie de ChatController.java pour l'affichage des messages

    // Inner class pour les cellules de messages - VERSION CORRIGÉE
    private class ChatMessageCell extends ListCell<ChatMessage> {
        private HBox container = new HBox(10); // CORRECTION: Espacement entre éléments
        private ImageView avatarImageView = new ImageView();
        private VBox messageContentContainer = new VBox(2); // CORRECTION: Espacement réduit
        private Label senderLabel = new Label();
        private Label messageLabel = new Label(); // CORRECTION: Utiliser Label au lieu de TextFlow
        private Label timestampLabel = new Label();
        private static final double AVATAR_SIZE_CHAT = 30;

        public ChatMessageCell() {
            super();

            // Configuration avatar
            avatarImageView.setFitHeight(AVATAR_SIZE_CHAT);
            avatarImageView.setFitWidth(AVATAR_SIZE_CHAT);
            avatarImageView.setPreserveRatio(true);
            Circle clip = new Circle(AVATAR_SIZE_CHAT / 2, AVATAR_SIZE_CHAT / 2, AVATAR_SIZE_CHAT / 2);
            avatarImageView.setClip(clip);

            // CORRECTION: Configuration du message label pour affichage en ligne
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(400); // Largeur maximum
            messageLabel.getStyleClass().add("message-text-content");

            // Configuration des conteneurs
            messageContentContainer.getChildren().addAll(senderLabel, messageLabel);
            messageContentContainer.getStyleClass().add("message-bubble");

            // CORRECTION: Configuration du container principal
            container.setAlignment(Pos.CENTER_LEFT);
            container.setSpacing(10);
            container.setPadding(new Insets(3, 5, 3, 5));

            // Styles
            senderLabel.getStyleClass().add("message-sender");
            timestampLabel.getStyleClass().add("message-timestamp");
        }

        @Override
        protected void updateItem(ChatMessage message, boolean empty) {
            super.updateItem(message, empty);

            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                avatarImageView.setImage(null);
                return;
            }

            // CORRECTION: Configurer le contenu du message
            senderLabel.setText(message.getSender());

            // CORRECTION: Traitement des emojis et affichage du contenu
            String processedContent = replaceEmojiShortcodes(message.getContent());
            messageLabel.setText(processedContent);

            timestampLabel.setText(message.getFormattedTimestamp());

            // Configuration de l'avatar
            String avatarUrl = message.getSenderAvatarUrl();
            Image img = null;
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    img = new Image(avatarUrl, true);
                    avatarImageView.setImage(img);
                } catch (Exception e) {
                    System.err.println("Erreur chargement avatar: " + avatarUrl);
                    avatarImageView.setImage(null);
                }
            } else {
                avatarImageView.setImage(null);
            }
            avatarImageView.setVisible(img != null && !img.isError());

            // CORRECTION: Layout selon le type de message
            container.getChildren().clear();

            if (message.isSentByCurrentUser()) {
                // Messages envoyés - alignés à droite
                container.setAlignment(Pos.CENTER_RIGHT);
                messageContentContainer.getStyleClass().setAll("message-bubble", "sent");
                senderLabel.setVisible(false); // Masquer le nom pour ses propres messages
                container.getChildren().addAll(timestampLabel, messageContentContainer, avatarImageView);

            } else if (message.getType() == ChatMessage.MessageType.SYSTEM_NOTIFICATION ||
                    message.getType() == ChatMessage.MessageType.USER_EVENT) {
                // Messages système - centrés
                container.setAlignment(Pos.CENTER);
                messageContentContainer.getStyleClass().setAll("message-bubble", "system");
                senderLabel.setVisible(true);
                avatarImageView.setVisible(false);
                container.getChildren().addAll(messageContentContainer);
                timestampLabel.setVisible(false);

            } else {
                // Messages reçus - alignés à gauche
                container.setAlignment(Pos.CENTER_LEFT);
                messageContentContainer.getStyleClass().setAll("message-bubble", "received");
                senderLabel.setVisible(true);
                container.getChildren().addAll(avatarImageView, messageContentContainer, timestampLabel);
            }

            setGraphic(container);
        }
    }

    // Inner class for userListView cells
    private class UserListCell extends ListCell<String> {
        private HBox hbox = new HBox(10);
        private ImageView avatarView = new ImageView();
        private Label usernameLabel = new Label();
        private static final double USER_LIST_AVATAR_SIZE = 24;

        public UserListCell() {
            super();
            avatarView.setFitHeight(USER_LIST_AVATAR_SIZE);
            avatarView.setFitWidth(USER_LIST_AVATAR_SIZE);
            avatarView.setPreserveRatio(true);
            Circle clip = new Circle(USER_LIST_AVATAR_SIZE / 2, USER_LIST_AVATAR_SIZE / 2, USER_LIST_AVATAR_SIZE / 2);
            avatarView.setClip(clip);
            hbox.getChildren().addAll(avatarView, usernameLabel);
            hbox.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(String username, boolean empty) {
            super.updateItem(username, empty);
            if (empty || username == null) {
                setText(null);
                setGraphic(null);
                avatarView.setImage(null);
            } else {
                usernameLabel.setText(username);
                UserProfile userProfile = userProfilesCache.get(username);
                Image img = null;
                if (userProfile != null && userProfile.getAvatarUrl() != null && !userProfile.getAvatarUrl().isEmpty()) {
                    try {
                        img = new Image(userProfile.getAvatarUrl(), true);
                        avatarView.setImage(img);
                        img.errorProperty().addListener((obs, oldError, newError) -> {
                            if (newError) {
                                System.err.println("Error loading user list avatar for " + username + ": " + userProfile.getAvatarUrl());
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Exception creating image for user list avatar " + username + ": " + e.getMessage());
                        avatarView.setImage(null);
                    }
                } else {
                    avatarView.setImage(null);
                }
                avatarView.setVisible(avatarView.getImage() != null);
                setGraphic(hbox);
            }
        }
    }

    private void updateUserListContext(String newContext) {
        // Vider la liste des utilisateurs pour le nouveau contexte
        activeUsernames.clear();

        // Mettre à jour l'affichage selon le contexte
        if (currentDmPartner != null) {
            // En mode DM, pas d'affichage de liste
            welcomeLabel.setText("DM with: " + currentDmPartner);
        } else if ("public".equals(newContext) || newContext.isEmpty()) {
            welcomeLabel.setText("Welcome, " + currentUsername + " - Public Chat");
        } else {
            welcomeLabel.setText("Welcome, " + currentUsername + " - Code: " + newContext);
        }
    }
}
