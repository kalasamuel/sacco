package com.pahappa.sacco.service;

import com.pahappa.sacco.dao.UserDao;
import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.security.PasswordUtil;
import com.pahappa.sacco.util.TransactionUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class UserService {

    private final UserDao userDao;

    @Inject
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    protected UserService() {
        this.userDao = null;
    }

    public boolean usernameExists(String username) {
        return userDao.findByUsername(username).isPresent();
    }

    public String generateUniqueUsername(String firstName, String lastName, String memberNumber) {
        String base = (firstName + lastName + memberNumber).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }
        String candidate = base;
        int suffix = 1;
        while (usernameExists(candidate)) {
            String suffixStr = String.valueOf(suffix++);
            int maxBaseLength = 30 - suffixStr.length();
            candidate = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) + suffixStr : base + suffixStr;
        }
        return candidate;
    }

    public User createSystemUser(Member member, String username, String plainPassword, Role role) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleViolationException("Username must not be empty.");
        }
        if (usernameExists(username)) {
            throw new BusinessRuleViolationException("Username already exists.");
        }
        String passwordHash = PasswordUtil.hash(plainPassword);
        User user = new User(username, passwordHash, member.getFullName(), role);
        user.setMemberProfile(member);
        userDao.save(user);
        return user;
    }

    public User updateUsernameAndPassword(User user, String requestedUsername, String newPassword) {
        if (requestedUsername == null || requestedUsername.isBlank()) {
            throw new BusinessRuleViolationException("Username must not be empty.");
        }
        if (!requestedUsername.equalsIgnoreCase(user.getUsername()) && usernameExists(requestedUsername)) {
            throw new BusinessRuleViolationException("Username already exists.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessRuleViolationException("Password must not be empty.");
        }
        user.setUsername(requestedUsername);
        user.setPasswordHash(PasswordUtil.hash(newPassword));
        return TransactionUtil.execute(em -> userDao.update(user));
    }
}
