CREATE DATABASE IF NOT EXISTS dating_app_db;
USE dating_app_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- BCrypt hashes are typically 60 chars, but 255 is safe
    email VARCHAR(100) UNIQUE NOT NULL,
    avatar_url VARCHAR(512) DEFAULT NULL,
    bio TEXT DEFAULT NULL, -- New column for user biography
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT, 
    meeting_code VARCHAR(100), 
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);
