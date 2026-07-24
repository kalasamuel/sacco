package com.pahappa.sacco.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {

    private static final String SESSION_TOKEN_KEY = "csrfToken";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    public static String getOrCreateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(SESSION_TOKEN_KEY);
        if (token == null) {
            token = generateToken();
            session.setAttribute(SESSION_TOKEN_KEY, token);
        }
        return token;
    }

    public static boolean isValid(HttpServletRequest request, String submittedToken) {
        HttpSession session = request.getSession(false);
        if (session == null || submittedToken == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(SESSION_TOKEN_KEY);
        return sessionToken != null && constantTimeEquals(sessionToken, submittedToken);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
