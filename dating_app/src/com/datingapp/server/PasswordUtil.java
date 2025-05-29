package com.datingapp.server;

// Remove old imports if no longer needed (java.nio, java.security, java.util.Base64)
// Add BCrypt import
import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Hashes a password using BCrypt.
     *
     * @param plainPassword The password to hash.
     * @return The BCrypt hashed password string.
     */
    public static String hashPassword(String plainPassword) {
        // The gensalt method can take a log_rounds parameter (e.g., 12)
        // Higher rounds = more secure but slower. Default is 10.
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); 
    }

    /**
     * Checks a plain password against a BCrypt hashed password.
     *
     * @param plainPassword  The plain password to check.
     * @param hashedPassword The BCrypt hashed password from the database.
     * @return True if the password matches, false otherwise.
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            // Basic check to see if it looks like a BCrypt hash.
            // Old SHA-256 hashes won't start with $2a$ (or $2b$, $2y$).
            // This prevents BCrypt.checkpw from throwing an exception with old hashes.
            System.err.println("Warning: Attempting to check password against a non-BCrypt hash: " + hashedPassword);
            return false; 
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Handles cases where the hash is not a valid BCrypt string, though the prefix check helps
            System.err.println("Error checking password with BCrypt (invalid hash format?): " + e.getMessage());
            return false;
        }
    }
}
