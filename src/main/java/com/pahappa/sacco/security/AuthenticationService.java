package com.pahappa.sacco.security;

import com.pahappa.sacco.dao.UserDao;
import com.pahappa.sacco.entity.AuditLog;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.util.TransactionUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

//Authenticates staff logins
@ApplicationScoped
public class AuthenticationService {

    private final LoginAttemptService loginAttemptService;
    private final UserDao userDao;

    @Inject
    public AuthenticationService(LoginAttemptService loginAttemptService, UserDao userDao) {
        this.loginAttemptService = loginAttemptService;
        this.userDao = userDao;
    }

    protected AuthenticationService() {
        this.loginAttemptService = null;
        this.userDao = null;
    }

    public static class AuthenticationResult {
        public final boolean success;
        public final User user;
        public final String errorMessage;

        private AuthenticationResult(boolean success, User user, String errorMessage) {
            this.success = success;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        static AuthenticationResult ok(User user) {
            return new AuthenticationResult(true, user, null);
        }

        static AuthenticationResult fail(String message) {
            return new AuthenticationResult(false, null, message);
        }
    }

    public AuthenticationResult authenticate(String username, String plainPassword) {
        if (username == null || username.isBlank() || plainPassword == null || plainPassword.isBlank()) {
            return AuthenticationResult.fail("Invalid username or password.");
        }

        if (loginAttemptService.isLocked(username)) {
            return AuthenticationResult.fail(
                    "This account is temporarily locked due to repeated failed login attempts. Try again later.");
        }

        Optional<User> found = userDao.findByUsername(username);
        String hashToCheck = found.map(User::getPasswordHash)
                .orElse("$2a$12$CvT2C6C6xF4L2q0J0ZzsEeC0Jc0m7QeYV1s0m2u3q4Z5b6C7d8E9F");

        boolean passwordMatches = PasswordUtil.verify(plainPassword, hashToCheck);

        if (found.isEmpty() || !passwordMatches || !Boolean.TRUE.equals(found.get().getActive())) {
            loginAttemptService.recordFailure(username);
            return AuthenticationResult.fail("Invalid username or password.");
        }

        loginAttemptService.recordSuccess(username);
        User user = found.get();

        TransactionUtil.executeVoid(entityManager ->
                entityManager.persist(new AuditLog(user, "USER", user.getId(), "LOGIN_SUCCESS", null)));

        return AuthenticationResult.ok(user);
    }
}
