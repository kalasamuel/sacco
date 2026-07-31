package com.kimwanyi.service;

import java.util.List;
import java.util.stream.Collectors;

import com.kimwanyi.service.interfaces.AuditLogService;
import com.kimwanyi.service.interfaces.EmailService;
import com.kimwanyi.exception.ResourceNotFoundException;
import com.kimwanyi.util.converter.UserAccountConverter;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.UserAccountDAO;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.UserAccountDto;
import com.kimwanyi.model.enums.AuditAction;
import com.kimwanyi.model.enums.MemberStatus;
import com.kimwanyi.model.enums.Role;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kimwanyi.service.interfaces.UserManagementService;


@Service
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    private final UserAccountDAO userAccountDAO;
    private final UserAccountConverter userAccountConverter;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final MemberDAO memberDAO;
    private final EmailService emailService;

    public UserManagementServiceImpl(UserAccountDAO userAccountDAO, UserAccountConverter userAccountConverter, AuditLogService auditLogService, PasswordEncoder passwordEncoder,
                         MemberDAO memberDAO, EmailService emailService) {
        this.userAccountDAO = userAccountDAO;
        this.userAccountConverter = userAccountConverter;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.memberDAO = memberDAO;
        this.emailService = emailService;
    }

    @Override
    public List<UserAccountDto> search(String keyword) {
        List<UserAccount> results;
        if (keyword == null || keyword.isBlank()) {
            results = userAccountDAO.findAll();
        } else {
            results = userAccountDAO.search(keyword.trim());
        }
        return results.stream()
                .map(account -> {
                    UserAccountDto dto = userAccountConverter.toDto(account);
                    if (account.getRole() == Role.MEMBER) {
                        memberDAO.findByUserAccountId(account.getId())
                                .ifPresent(member -> dto.setMemberStatus(member.getStatus().name()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setEnabled(Long userId, boolean enabled, UserAccount adminAccount) {
        requireEnabledAdmin(adminAccount);
        UserAccount account = userAccountDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found with ID: " + userId));

        if (!enabled && (account == adminAccount
                || (account.getId() != null && account.getId().equals(adminAccount.getId())))) {
            throw new IllegalArgumentException("An administrator cannot disable their own account");
        }

        if (account.isEnabled() == enabled) {
            return; // No-op
        }

        account.setEnabled(enabled);
        userAccountDAO.save(account);

        if (account.getRole() == Role.MEMBER) {
            memberDAO.findByUserAccountId(account.getId()).ifPresent(member -> {
                member.setStatus(enabled ? MemberStatus.ACTIVE : MemberStatus.INACTIVE);
                memberDAO.save(member);
            });
        }

        AuditAction action = enabled ? AuditAction.USER_ACCOUNT_ENABLED : AuditAction.USER_ACCOUNT_DISABLED;
        auditLogService.record(adminAccount, action, "UserAccount", userId, null);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword, UserAccount adminAccount) {
        requireEnabledAdmin(adminAccount);
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        UserAccount account = userAccountDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found with ID: " + userId));

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountDAO.save(account);

        auditLogService.record(adminAccount, AuditAction.PASSWORD_CHANGED, "UserAccount", userId, "Reset by admin");
    }

    @Override
    @Transactional
    public void approveMember(Long userId, UserAccount adminAccount) {
        requireEnabledAdmin(adminAccount);
        UserAccount account = userAccountDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        if (account.getRole() != Role.MEMBER) {
            throw new IllegalArgumentException("Only member accounts can be approved");
        }
        var member = memberDAO.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalArgumentException("This member application is not pending approval");
        }
        member.setStatus(MemberStatus.ACTIVE);
        account.setEnabled(true);
        memberDAO.save(member);
        userAccountDAO.save(account);
        registerApprovalPostCommitActions(adminAccount, account, member);
    }

    private void registerApprovalPostCommitActions(UserAccount adminAccount,
                                                   UserAccount account,
                                                   com.kimwanyi.model.Member member) {
        Runnable actions = () -> {
            try {
                auditLogService.record(adminAccount, AuditAction.MEMBER_APPROVED, "Member", member.getId(),
                        "Approved membership " + member.getMembershipNumber());
            } catch (RuntimeException auditFailure) {
                logger.error("Could not record approval audit log for member {}", member.getId(), auditFailure);
            }

            try {
                emailService.sendToMembers(List.of(member.getId()),
                        "Your Kimwanyi SACCO account has been approved",
                        "Hello " + account.getFirstName() + ",\n\n"
                                + "Your Kimwanyi SACCO membership has been verified and approved. "
                                + "Log in using your registered username.\n\n"
                                + "Membership number: " + member.getMembershipNumber() + "\n\n"
                                + "Welcome to Kimwanyi SACCO.",
                        adminAccount);
            } catch (RuntimeException emailFailure) {
                try {
                    auditLogService.record(adminAccount, AuditAction.EMAIL_FAILED, "Member", member.getId(),
                            "Approval email could not be sent: " + emailFailure.getMessage());
                } catch (RuntimeException auditFailure) {
                    logger.error("Could not record approval email failure for member {}", member.getId(), auditFailure);
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    actions.run();
                }
            });
        } else {
            actions.run();
        }
    }

    private void requireEnabledAdmin(UserAccount account) {
        if (account == null || account.getRole() != Role.ADMIN || !account.isEnabled()) {
            throw new SecurityException("An enabled administrator account is required");
        }
    }
}
