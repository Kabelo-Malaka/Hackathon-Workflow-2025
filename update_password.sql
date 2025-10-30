UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMye/EFvDK.b6.6KuQFjcrdQNKz4XvHIHwu' WHERE username = 'admin';
SELECT username, password_hash FROM users WHERE username = 'admin';
