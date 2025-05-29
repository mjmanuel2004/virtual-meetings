# Dating Application - User Acceptance Testing (UAT) Guide

## 1. Introduction

This guide provides step-by-step scenarios for manually testing the functionality of the Java Dating Application. The goal is to ensure all features are working as expected.

**Prerequisites for Testing:**
*   Ensure your Java Development Kit (JDK) and JavaFX SDK are correctly set up as per the main `README.md`.
*   Docker and Docker Compose must be installed and running.
*   All required JAR dependencies (MySQL Connector, WebSocket API, Tyrus, jBCrypt) must be in the `dating_app/lib/` folder.
*   The server application (`MainServer.java`) must be compiled and ready to run.
*   The client application (`MainClient.java`) must be compiled and ready to run.
*   You might need two instances of the client application running to test interactions between users.

## 2. Server & Database Setup Verification

1.  **Start Database:**
    *   Open a terminal in the `dating_app/` directory.
    *   Run `docker-compose down -v` (if you want to ensure a clean start, this removes old data).
    *   Run `docker-compose up -d`.
    *   Verify containers are running: `docker ps`. You should see `mysql_dating_app` and `phpmyadmin_dating_app`.
    *   Access phpMyAdmin via `http://localhost:8081` (user: `root`, pass: `rootpassword`, server: `mysql_db`). Check that the `dating_app_db` database and its tables (`users`, `messages`) exist. Verify the `avatar_url` and `bio` columns exist in the `users` table.
2.  **Start Server Application:**
    *   Open another terminal in the `dating_app/` directory.
    *   Compile the server (if not already done, see `README.md`).
    *   Run the server: `java -cp "bin:lib/*" com.datingapp.MainServer` (or equivalent for your OS).
    *   Expected: Server starts without errors, indicating WebSocket server is running on the configured port (default 8025 from `server.properties`).

## 3. User Registration

*You will need to run the client application (`MainClient.java`) for these tests.*

**Scenario 3.1: Successful New User Registration**
1.  Launch the client application.
2.  On the Login screen, click "Don't have an account? Register".
3.  Enter a unique username (e.g., `testuser1`), a valid email (e.g., `testuser1@example.com`), and a password (e.g., `password123`). Confirm the password.
4.  Click "Register".
5.  **Expected:** A success message is displayed (e.g., "Registration successful. Please login."). Check phpMyAdmin: a new user should be in the `users` table with a BCrypt hashed password (it will look like `$2a$....`).

**Scenario 3.2: Attempt to Register with an Existing Username**
1.  Launch the client. Go to the Registration screen.
2.  Enter the same username used in 3.1 (e.g., `testuser1`), a *different* email (e.g., `another@example.com`), and a password.
3.  Click "Register".
4.  **Expected:** An error message is displayed indicating the username already exists.

**Scenario 3.3: Attempt to Register with an Existing Email**
1.  Launch the client. Go to the Registration screen.
2.  Enter a *new* username (e.g., `testuser2`), the same email used in 3.1 (e.g., `testuser1@example.com`), and a password.
3.  Click "Register".
4.  **Expected:** An error message is displayed indicating the email already exists.

## 4. User Login

**Scenario 4.1: Successful Login**
1.  Launch the client. Use the credentials of a user registered in step 3.1 (e.g., `testuser1`, `password123`).
2.  Click "Login".
3.  **Expected:** Login is successful. The chat view appears. The welcome message should display your username.

**Scenario 4.2: Login with Incorrect Password**
1.  Launch the client. Enter a valid username (e.g., `testuser1`) but an incorrect password.
2.  Click "Login".
3.  **Expected:** An error message like "Invalid username or password" or "Invalid credentials" is displayed.

**Scenario 4.3: Login with Non-existent Username**
1.  Launch the client. Enter a username that has not been registered (e.g., `nouser`).
2.  Click "Login".
3.  **Expected:** An error message like "Invalid username or password" or "User not found" is displayed.

## 5. Profile Management (Avatar & Bio)

*Assumes `testuser1` is logged in.*

**Scenario 5.1: Set a Valid Avatar URL**
1.  In the Chat View, click the "Profile" button.
2.  In the Profile dialog, enter a valid direct image URL into the "Avatar URL" field (e.g., a link to a PNG or JPG image like `https://www.example.com/someimage.png`).
3.  Click "Save Avatar URL".
4.  **Expected:** A success message ("Avatar URL updated successfully!") appears. The URL is saved. (Verify in `users` table via phpMyAdmin if desired).
5.  Close the Profile dialog. (Avatars should appear in subsequent messages/user list updates).

**Scenario 5.2: Set an Invalid/Empty Avatar URL**
1.  Open the Profile dialog again.
2.  Try setting an invalid URL (e.g., `justsometext`) or a very long string. Click "Save".
3.  **Expected:** An error message regarding invalid format or length should appear.
4.  Clear the URL field and click "Save".
5.  **Expected:** Success message (empty URL is valid for removing avatar). The `avatar_url` in DB should be NULL or empty.

**Scenario 5.3: Set a Bio**
1.  Log in as `testuser1`. Open the Profile dialog.
2.  Enter text into the "Bio" text area (e.g., "Loves coding and long walks on the beach. My favorite emoji is :D").
3.  Click "Save Bio".
4.  **Expected:** Success message ("Profile bio updated successfully!"). Bio is saved (verify in `users` table via phpMyAdmin). The bio text area should retain the saved bio.

**Scenario 5.4: Update an Existing Bio**
1.  Open Profile dialog again. Bio field should show current bio.
2.  Change the bio text (e.g., "Updated bio with new info and another emoji <3"). Click "Save Bio".
3.  **Expected:** Success message. New bio is saved.

**Scenario 5.5: Clear Bio**
1.  Open Profile dialog. Clear the bio text area. Click "Save Bio".
2.  **Expected:** Success message. Bio is saved as empty/NULL in database.

## 6. Public / Meeting Code Chat

*You'll need at least two clients for full testing. Register and log in `testuser1` on Client A and `testuser2` on Client B.*

**Scenario 6.1: Send/Receive in Public Chat**
1.  Both users (`testuser1`, `testuser2`) are logged in. By default, they are in the "public" chat.
2.  `testuser1` types "Hello public from user1 with an emoji :)" and sends.
3.  **Expected:** `testuser1` sees their message. `testuser2` sees "testuser1: Hello public from user1 with an emoji ☺". Both messages should have timestamps. Avatars (if set) should appear. The emoji shortcode should be rendered as ☺.
4.  `testuser2` replies with "Got it! How about <3 and :D?".
5.  **Expected:** Both users see the reply with emojis ❤ and 😃 rendered. Verify symmetrical behavior.

**Scenario 6.2: Join/Create a Specific Meeting Code**
1.  `testuser1` enters "meeting123" in the "Meeting Code" field and clicks "Join/Set Code".
2.  **Expected:** `testuser1`'s chat view updates (e.g., label indicates "Chat - Meeting Code: meeting123"), chat area clears. Message history for "meeting123" (if any) loads.
3.  `testuser2` types "secret message in public" and sends.
4.  **Expected:** `testuser1` (in "meeting123") should NOT see this message.
5.  `testuser2` also joins "meeting123".
6.  **Expected:** `testuser2`'s view updates, history for "meeting123" loads.

**Scenario 6.3: Chat within Specific Meeting Code**
1.  Both users are in "meeting123".
2.  `testuser1` sends "Hello from meeting123".
3.  **Expected:** Both `testuser1` and `testuser2` see this message. Avatars and timestamps present.
4.  Another user, `testuser3` (if you register one), logs in and stays in "public" chat.
5.  **Expected:** `testuser3` does NOT see messages from "meeting123".

**Scenario 6.4: Message History Loading**
1.  Send a few messages in "public" between `testuser1` and `testuser2`.
2.  `testuser1` logs out and logs back in.
3.  **Expected:** Upon logging into public chat, `testuser1` should see the recent message history for the public chat.
4.  Repeat for a specific meeting code: send messages, have one user leave the code and rejoin (or logout/login and rejoin code). History should load.

## 7. Direct Messaging (DM)

*Assumes `testuser1` and `testuser2` are logged in and in the public chat space (visible in each other's user lists).*

**Scenario 7.1: Initiate a DM**
1.  On `testuser1`'s client, click `testuser2` in the user list.
2.  **Expected:** `testuser1`'s chat view changes to "Direct Message with: testuser2". Chat area clears. DM history (if any) loads. Meeting code input is disabled.

**Scenario 7.2: Send and Receive DMs**
1.  `testuser1` types "Hi testuser2, this is a DM!" and sends.
2.  **Expected:**
    *   `testuser1` sees their sent message in the DM view with `testuser2`.
    *   `testuser2` (who is not yet in a DM view with testuser1) should see this message appear if they subsequently enter DM mode with `testuser1`. *Self-correction: The current implementation adds DM to current view if context matches, otherwise it's just stored. For full test, `testuser2` should also click on `testuser1` to enter DM mode with them to see messages.*
    *   Let `testuser2` now click on `testuser1` from their user list to also enter DM mode.
    *   `testuser2` replies "Hi testuser1, I got your DM! How about this emoji: ;)?".
    *   **Expected:** Both users see the conversation in their DM view with each other. Avatars and timestamps present. `testuser2`'s message should render with the 😉 emoji.

**Scenario 7.3: DM History Loading**
1.  Exchange a few DMs between `testuser1` and `testuser2`.
2.  `testuser1` switches to public chat (by entering "public" or empty in meeting code and clicking "Join/Set Code").
3.  `testuser1` then clicks `testuser2` in the user list again.
4.  **Expected:** The recent DM history with `testuser2` is loaded.

**Scenario 7.4: DM context isolation**
1.  `testuser1` is in DM with `testuser2`. `testuser3` logs in and sends a public message.
2.  **Expected:** `testuser1` should NOT see `testuser3`'s public message while in the DM view with `testuser2`.

## 8. User List & Presence

**Scenario 8.1: User List Updates**
1.  Client A: `testuser1` is logged in.
2.  Client B: `testuser2` logs in.
3.  **Expected (Client A):** `testuser2` appears in the user list with their avatar (if set).
4.  Client B: `testuser2` logs out.
5.  **Expected (Client A):** `testuser2` disappears from the user list.
6.  Verify avatars in user list are displayed correctly if users have set them.

## 9. Server Configuration (`server.properties` - Advanced/Optional)

**Scenario 9.1: Change Server Port**
1.  Stop the `MainServer`.
2.  Edit `dating_app/server.properties`. Change `server.port` from `8025` to `8026` (or another free port).
3.  Start `MainServer` again.
4.  **Expected:** Server logs indicate it's running on the new port (`8026`).
5.  Launch `MainClient`.
6.  **Expected:** Client fails to connect (as it's likely hardcoded or also needs config for server URI). *Self-correction: `WebSocketClientService` has hardcoded port. This test highlights that client also needs configuration for server URI, or the `README.md` needs to be very clear about this if client is not configurable.* For now, this test mainly verifies server uses the property.

## 10. Error Handling Examples

*   Attempt to register with an invalid email format (e.g., "test@test"). Expected: Client-side or server-side validation error.
*   While client is running, stop the `MainServer`. Try to send a message. Expected: Client should indicate a disconnection or failure to send.
*   Set an avatar URL that points to a non-image resource. Expected: Avatar display should fail gracefully (e.g., show placeholder or nothing, no crash).

---
This guide should be saved as `dating_app/docs/TESTING_GUIDE.md`.
```
