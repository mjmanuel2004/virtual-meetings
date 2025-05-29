# Java Dating Application - Proof of Concept

## 1. Overview

This application is a proof-of-concept for a Java-based dating/messaging system. It features a client-server architecture where users can register, log in, and communicate via messages. Communication is handled through WebSockets, and user data is stored in a MySQL database. The client interface is built using JavaFX and FXML.

This project is developed **without Maven or Gradle**, requiring manual management of dependencies and compilation.

## 2. Prerequisites

Before you begin, ensure you have the following installed:

*   **Java Development Kit (JDK):** Version 11 or later is recommended. Ensure `java` and `javac` are in your system's PATH.
*   **Docker and Docker Compose:** For running the MySQL database and phpMyAdmin. ([Install Docker](https://docs.docker.com/get-docker/), [Install Docker Compose](https://docs.docker.com/compose/install/)).
*   **JavaFX SDK:** Download the JavaFX SDK appropriate for your OS and JDK version from [OpenJFX](https://openjfx.io/). You'll need the path to its `lib` directory.
*   **JAR Dependencies:** You will need to download the following JAR files and place them into the `dating_app/lib/` directory:
    *   **MySQL Connector/J:** The JDBC driver for MySQL. Download from [MySQL Community Downloads](https://dev.mysql.com/downloads/connector/j/). Get the "Platform Independent" ZIP or TAR, extract it, and find `mysql-connector-j-X.X.XX.jar`.
    *   **Java API for WebSocket (JSR 356):**
        *   `javax.websocket-api.jar`: Get version 1.1 from [Maven Central](https://search.maven.org/artifact/javax.websocket/javax.websocket-api/1.1/jar).
    *   **Tyrus (Reference Implementation for JSR 356):** For standalone WebSocket server and client.
        *   `tyrus-standalone-client-jdk.jar`: Get version 2.1.x (e.g., 2.1.5) from [Maven Central](https://search.maven.org/artifact/org.glassfish.tyrus.bundles/tyrus-standalone-client-jdk). This bundle often includes server capabilities for standalone use or you might need `tyrus-server.jar` and `tyrus-container-jdk-bundle.jar` if you want to run a more dedicated Tyrus server setup. For simplicity, the `tyrus-standalone-client-jdk` often suffices for basic server endpoints in a standalone app.
        *   _Note: Ensure you download the correct Tyrus JARs that provide both client and server capabilities for standalone execution if not using an embedded servlet container._
    *   **jBCrypt:** For hashing passwords securely. Download `jbcrypt-0.4.jar` from [Maven Central](https://search.maven.org/artifact/org.mindrot/jbcrypt/0.4/jar) (or search for `org.mindrot jbcrypt 0.4`). Place this JAR in the `dating_app/lib/` directory. (This is crucial for the enhanced password security).

## 3. Project Structure

*   `dating_app/`
    *   `src/`: Contains all Java source code (`.java` files).
        *   `com/datingapp/`: Main package.
            *   `client/`: Client-specific code (UI controllers, services, models).
            *   `server/`: Server-specific code (WebSocket endpoint, DB utils).
            *   `common/`: (Currently unused, for shared models if any).
            *   `MainClient.java`: Entry point for the client application.
            *   `MainServer.java`: Entry point for the server application.
    *   `resources/`: Contains FXML files, CSS stylesheets, and other non-code assets.
        *   `fxml/`: Stores `.fxml` UI layout files.
        *   `css/styles.css`: Stylesheet for the application's look and feel.
    *   `lib/`: **(Create this directory)** Place all downloaded JAR dependencies here.
    *   `docs/`: (Currently unused, for additional documentation).
    *   `docker-compose.yml`: Docker configuration for MySQL and phpMyAdmin.
    *   `init.sql`: SQL script for initial database schema creation.
    *   `README.md`: This file.
    *   `server.properties`: Configuration file for server settings (port, database credentials).

## 4. Server Setup

### 4.1. Database Setup

1.  Navigate to the `dating_app/` directory in your terminal.
2.  Run `docker-compose up -d` to start the MySQL database and phpMyAdmin containers in detached mode.
    *   MySQL will be accessible on port `3306`.
    *   phpMyAdmin will be accessible at `http://localhost:8081`. You can log in with user `root` and password `rootpassword` (server: `mysql_db`). The application database is `dating_app_db`, user `dating_app_user`, password `userpassword`.
    *   **Note on Schema Changes (Avatars & Bio):** The `users` table now includes an `avatar_url` column and a `bio` column.
        *   For **new setups**, `init.sql` already includes these columns.
        *   For **existing setups** where you previously ran the application without these columns, you need to update your database schema manually or by resetting the database volume.
            *   **Manual Alter (if you want to preserve data):** Connect to your `dating_app_db` (e.g., via phpMyAdmin) and run:
                *   `ALTER TABLE users ADD COLUMN avatar_url VARCHAR(512) DEFAULT NULL;` (if not already added)
                *   `ALTER TABLE users ADD COLUMN bio TEXT DEFAULT NULL;`
            *   **Reset Volume (if starting fresh is okay):** Stop Docker (`docker-compose down`), remove the MySQL volume (`docker volume rm dating_app_mysql_data` or the name shown in `docker volume ls`), and then restart (`docker-compose up -d`). This will re-initialize the database using the updated `init.sql`.

### 4.2. Application Setup & Running

1.  **Place JARs:** Ensure all required JARs (MySQL Connector, WebSocket API, Tyrus) are in the `dating_app/lib/` directory.
2.  **Compile:**
    *   Open a terminal in the `dating_app/` directory.
    *   Create a directory for compiled classes if it doesn't exist, e.g., `mkdir bin`.
    *   Compile the server code:
        ```bash
        # Adjust path separators for Windows (\) if necessary
        # Ensure all JARs from lib are included in classpath
        javac -d bin -cp "lib/*:." src/com/datingapp/server/*.java src/com/datingapp/*.java
        ```
        *(Note: If you have common classes, ensure they are compiled too. The wildcard `lib/*` works on Linux/macOS; for Windows, you might need to list each JAR or use a more complex classpath string.)*
3.  **Run:**
    *   Run the server:
        ```bash
        java -cp "bin:lib/*" com.datingapp.MainServer
        ```
    *   The server will attempt to start the WebSocket endpoint. The `MainServer.java` currently has conceptual code for Tyrus. You may need to adjust `MainServer.java` to correctly initialize and start the Tyrus server with `ChatServerEndpoint.class`. For example:
        ```java
        // Inside MainServer.java main method (ensure Tyrus JARs are in lib):
        // import org.glassfish.tyrus.server.Server;
        // import com.datingapp.server.ChatServerEndpoint;
        
        // Server server = new Server("localhost", 8025, "/websockets", null, ChatServerEndpoint.class);
        // try {
        //     server.start();
        //     System.out.println("WebSocket server started on ws://localhost:8025/websockets");
        //     System.out.println("Press any key to stop the server...");
        //     System.in.read();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // } finally {
        //     if (server != null) server.stop();
        // }
        ```
    *   You should see a "Server Application Started" message, followed by WebSocket server startup messages if Tyrus is correctly configured and started.

### 4.3. Server Configuration (`server.properties`)

The server can be configured via the `dating_app/server.properties` file. This file allows you to set:

*   `db.url`: The JDBC URL for the MySQL database.
    *   Example: `jdbc:mysql://localhost:3306/dating_app_db`
*   `db.user`: The username for the database.
    *   Example: `dating_app_user`
*   `db.password`: The password for the database user.
    *   Example: `userpassword`
*   `server.port`: The port on which the WebSocket server will listen.
    *   Example: `8025`

Modify this file to match your environment if you are not using the default Docker setup or want to change the server port. If the file is not found or a property is missing, the server will attempt to use hardcoded default values.

## 5. Client Setup

### 5.1. Application Setup & Running

1.  **Place JARs:** Ensure `tyrus-standalone-client-jdk.jar` (or your chosen Tyrus client bundle) and all other required JARs (`mysql-connector-java.jar`, `javax.websocket-api.jar`, `jbcrypt-0.4.jar`) are present in the `dating_app/lib/` directory. The JavaFX SDK libraries will be referenced from where you downloaded them.
2.  **Compile:**
    *   Open a terminal in the `dating_app/` directory.
    *   **Set `JAVAFX_SDK_LIB`:** Define an environment variable `JAVAFX_SDK_LIB` pointing to the `lib` directory of your JavaFX SDK.
        *   **Windows Example (Command Prompt):** `set JAVAFX_SDK_LIB=C:\path\to\javafx-sdk-XX\lib`
        *   **Windows Example (PowerShell):** `$env:JAVAFX_SDK_LIB="C:\path\to\javafx-sdk-XX\lib"`
        *   **Linux/macOS Example (Bash):** `export JAVAFX_SDK_LIB=/path/to/javafx-sdk-XX/lib`
        *   Alternatively, you can directly substitute the path in the compile command. For example, if your JavaFX SDK's lib directory is `C:\javafx-sdk-17\lib` on Windows, replace `%JAVAFX_SDK_LIB%` (or `$JAVAFX_SDK_LIB` on Linux/macOS) with this absolute path in the command below.
    *   Compile the client code (ensure `bin` directory exists or is created):
        ```bash
        # For Windows (adjust JAVAFX_SDK_LIB path if not using env var):
        javac --module-path "%JAVAFX_SDK_LIB%" --add-modules javafx.controls,javafx.fxml -d bin -cp "lib/*;." src/com/datingapp/client/ui/*.java src/com/datingapp/client/services/*.java src/com/datingapp/client/model/*.java src/com/datingapp/*.java

        # For Linux/macOS (adjust JAVAFX_SDK_LIB path if not using env var):
        # javac --module-path "$JAVAFX_SDK_LIB" --add-modules javafx.controls,javafx.fxml -d bin -cp "lib/*:." src/com/datingapp/client/ui/*.java src/com/datingapp/client/services/*.java src/com/datingapp/client/model/*.java src/com/datingapp/*.java
        ```
        *   **Classpath Note (`lib/*`):** The `lib/*` wildcard should include all JARs in the `lib` folder. This syntax is generally supported in modern Java versions when the command is run from a shell that performs the expansion (like Bash) or by Java itself if the classpath string is quoted (e.g., `-cp "lib/*:."`). If `lib/*` does not work on your system's command line (especially older Windows `cmd.exe`), you may need to list each JAR explicitly, separated by your OS's path separator (`;` for Windows, `:` for Linux/macOS). Ensure all required JARs are included.
        *   Also note the inclusion of `src/com/datingapp/client/model/*.java` in the compilation path.
3.  **Run:**
    *   Run the client:
        ```bash
        # For Windows (adjust JAVAFX_SDK_LIB path if not using env var):
        java --module-path "%JAVAFX_SDK_LIB%" --add-modules javafx.controls,javafx.fxml -cp "bin;lib/*" com.datingapp.MainClient

        # For Linux/macOS (adjust JAVAFX_SDK_LIB path if not using env var):
        # java --module-path "$JAVAFX_SDK_LIB" --add-modules javafx.controls,javafx.fxml -cp "bin:lib/*" com.datingapp.MainClient
        ```
    *   The client application window should appear.

#### 5.1.1. Important: Server URI Configuration

Please note that the WebSocket server URI (including the server address and port) that the client attempts to connect to is currently hardcoded in the `dating_app/src/com/datingapp/client/services/WebSocketClientService.java` file (e.g., `ws://localhost:8025/...`).

If you change the `server.port` in the `server.properties` file for the server, you **must** manually update the URI in `WebSocketClientService.java` to match the new port and then recompile the client application. The client does not currently read this information from an external configuration file.

## 2.5 Key Features (Phase 2)

*   **Modern User Interface:** Styled with CSS for a clean, dark theme.
*   **Enhanced Chat Display:** Messages are styled distinctly for sender and receiver, including timestamps for better readability.
*   **Direct Messaging (DM):** Users can click on a username in the active user list to initiate a private one-on-one conversation.
*   **User Presence:** The user list displays currently online users (with their avatars) available for direct messaging.
*   **Meeting Code Chats:** Join or create topic-based chats using meeting codes.
*   **Message History:** Automatically loads recent message history when you open a Direct Message or join a meeting code chat.
*   **Secure Password Storage (BCrypt):** User passwords are now hashed using the strong BCrypt algorithm.
*   **User Avatars (via URL):** Users can set a URL for their avatar, which is then displayed in chat messages and the user list.
*   **Expanded User Profiles:** Users can add a personal bio via their profile settings.
*   **Emoji Support:** Basic emoji rendering in chat by typing common shortcodes (e.g., `:D`, `<3`).
*   **User Authentication:** Secure registration and login.
*   **Externalized Configuration:** Server settings (port, database) managed via `server.properties`.

## 6. Basic Usage

1.  **Launch Client:** Run `MainClient` as described above.
2.  **Register:**
    *   If you are a new user, click the "Don't have an account? Register" link.
    *   Fill in your desired username, email, and password. Click "Register".
    *   You should see a success message.
3.  **Login:**
    *   On the login screen, enter your registered username and password. Click "Login".
    *   Upon successful login, you will be taken to the chat view.
4.  **Chatting (Public/Meeting Code):**
    *   The main chat area displays messages with timestamps. Your messages will appear on the right, styled differently from messages received from others (which appear on the left).
    *   When you join a meeting code, the recent message history for that code will be displayed, allowing you to catch up on the conversation.
    *   Type your message in the input field at the bottom and click "Send" or press Enter.
    *   Your messages will appear, prefixed by your username.
5.  **Meeting Codes:**
    *   To join or create a specific "chat room" or context, type a code into the "Meeting Code" field (e.g., `friends_chat`, `project_alpha`) and click "Join/Set Code".
    *   Messages you send will then only be visible to others who have joined the same meeting code. Recent history for this code will load.
    *   To return to the public chat, you can try entering a common code like "public" or clearing the meeting code field (behavior might depend on server implementation for empty code). This will also exit any active DM session.

### 6.1. Direct Messaging (DM)

1.  **View Online Users:** The list on the right-hand side of the chat view shows users currently online in the public space, along with their avatars.
2.  **Initiate DM:** Click on a username in this list to start a direct message conversation with them. The chat view will then switch to DM mode, indicating who you are messaging. When you start a DM with a user, the recent conversation history will be automatically loaded. Meeting code functions will be disabled while in a DM.
3.  **Send/Receive DMs:** Messages sent while in DM mode are private between you and the selected user. Avatars are displayed next to messages.
4.  **Exit DM Mode:** To exit DM mode and return to public or meeting code chat, enter a meeting code (or leave it blank for "public") and click "Join/Set Code". This will switch your context out of the DM.

### 6.2. Editing Your Profile (Avatar & Bio)

1.  **Open Profile:** In the chat view, click the "Profile" button (usually located near the meeting code input).
2.  **Avatar URL:**
    *   **Enter URL:** In the "Avatar URL" field, paste the direct URL to your desired avatar image (e.g., `https://example.com/my_avatar.png`). The URL should point directly to an image file (common formats like PNG, JPG, GIF are generally supported by JavaFX).
    *   **Save:** Click "Save Avatar URL". You should see a confirmation message.
3.  **Bio:**
    *   **Enter Bio:** In the "Bio" text area, you can write a short description about yourself.
    *   **Save:** Click "Save Bio". You should see a confirmation message.
4.  **View Changes:** Your avatar (if set) will now appear next to your messages and in the user list for other users. Your bio is part of your profile data but is not currently displayed directly in the chat view (future enhancement).
5.  **Close:** Click "Close" to exit the profile dialog.

### 6.3. Using Emojis in Chat

You can use the following text shortcodes in your chat messages, which will be rendered as emojis:

*   `:)`  ->  ☺ (Smiley)
*   `:(`  ->  ☹ (Frowny)
*   `<3`  ->  ❤ (Heart)
*   `:D`  ->  😃 (Grinning Face with Big Eyes)
*   `;)`  ->  😉 (Winking Face)
*   `:P`  ->  😛 (Face with Stuck-out Tongue)
*   `:O`  ->  😮 (Face with Open Mouth)
*   `:'(` ->  😢 (Crying Face)
*   `xD`  ->  😂 (Face with Tears of Joy)

### 6.4. Important Security Update (BCrypt)

*   User passwords are now hashed using **BCrypt**, a strong and recommended password hashing algorithm.
*   **If you had registered users before this update (Phase 3), their old passwords (hashed with a simpler SHA-256 method) will no longer work.** Those users will need to re-register to have their passwords stored using the new BCrypt method. For a production system, a password migration strategy would be implemented, but for this proof-of-concept, re-registration is required.

6.  **Exiting:** Close the client window. Stop the server with Ctrl+C in its terminal or by pressing a key if `System.in.read()` is used. Stop Docker containers with `docker-compose down`.

---
This is a basic guide. Further development can include more features, enhanced UI, and more robust error handling.
```
