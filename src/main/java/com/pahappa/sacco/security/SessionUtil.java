package com.pahappa.sacco.security;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//  Single, consistent place that defines how the authenticated user is
//  stored in and read from the HTTP session. Every other class (filters,
//  managed beans) goes through this rather than touching session
//  attributes by raw string key directly

public final class SessionUtil {

    private static final String SESSION_USER_KEY = "authenticatedUser";

    private SessionUtil() {
    }

    public static void login(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        // Invalidate and recreate the session on login 
        session.invalidate();
        HttpSession freshSession = request.getSession(true);
        freshSession.setAttribute(SESSION_USER_KEY, user);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(SESSION_USER_KEY);
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    public static boolean hasRole(HttpServletRequest request, Role role) {
        User user = getCurrentUser(request);
        return user != null && user.getRole() == role;
    }
}
