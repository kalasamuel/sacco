package com.kimwanyi.service;

import com.kimwanyi.service.interfaces.AuthenticationService;
import com.kimwanyi.service.interfaces.AuditLogService;

import java.util.List;
import com.kimwanyi.exception.AuthenticationException;
import com.kimwanyi.dao.UserAccountDAO;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.LoggedInUserDto;
import com.kimwanyi.model.dto.forms.LoginForm;
import com.kimwanyi.model.enums.AuditAction;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";

    private final UserAccountDAO userAccountDAO;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthenticationServiceImpl(
            UserAccountDAO userAccountDAO,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userAccountDAO = userAccountDAO;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public LoggedInUserDto authenticate(LoginForm loginForm) {
        UserAccount userAccount = userAccountDAO.findByUsername(loginForm.getUsername())
                .orElse(null);
                
        if (userAccount == null) {
            auditLogService.record(null, AuditAction.LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername());
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!userAccount.isEnabled()) {
            auditLogService.record(null, AuditAction.LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername());
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(loginForm.getPassword(), userAccount.getPasswordHash())) {
            auditLogService.record(null, AuditAction.LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername());
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        auditLogService.record(userAccount, AuditAction.LOGIN_SUCCESS, "UserAccount", userAccount.getId(), null);

        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccount.getId());
        dto.setUsername(userAccount.getUsername());
        dto.setRoles(List.of(userAccount.getRole().name()));
        return dto;
    }
}
