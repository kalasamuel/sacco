package com.pahappa.sacco.security;

import org.mindrot.jbcrypt.BCrypt;

//Password hashing via BCrypt (jBCrypt).
public final class PasswordUtil {

    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
