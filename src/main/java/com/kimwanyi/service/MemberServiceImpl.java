package com.kimwanyi.service;

import java.math.BigDecimal;
import com.kimwanyi.util.MembershipNumberGenerator;
import com.kimwanyi.util.SavingsAccountNumberGenerator;
import com.kimwanyi.util.converter.MemberConverter;
import com.kimwanyi.dao.MemberDAO;
import com.kimwanyi.dao.SavingsAccountDAO;
import com.kimwanyi.dao.UserAccountDAO;
import com.kimwanyi.exception.ResourceNotFoundException;
import java.util.List;
import com.kimwanyi.model.Member;
import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.MemberDto;
import com.kimwanyi.model.dto.forms.MemberRegistrationForm;
import com.kimwanyi.model.dto.forms.MemberUpdateForm;
import com.kimwanyi.model.enums.AuditAction;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kimwanyi.service.interfaces.MemberService;
import com.kimwanyi.service.interfaces.AuditLogService;
import com.kimwanyi.service.interfaces.NotificationService;
import com.kimwanyi.service.interfaces.EmailService;

@Service
public class MemberServiceImpl implements MemberService {

    private static final Logger logger = LoggerFactory.getLogger(MemberServiceImpl.class);

    private final UserAccountDAO userAccountDAO;
    private final MemberDAO memberDAO;
    private final SavingsAccountDAO savingsAccountDAO;
    private final MemberConverter memberConverter;
    private final PasswordEncoder passwordEncoder;
    private final MembershipNumberGenerator membershipNumberGenerator;
    private final SavingsAccountNumberGenerator savingsAccountNumberGenerator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public MemberServiceImpl(
        UserAccountDAO userAccountDAO,
        MemberDAO memberDAO,
        SavingsAccountDAO savingsAccountDAO,
        MemberConverter memberConverter,
        PasswordEncoder passwordEncoder,
        MembershipNumberGenerator membershipNumberGenerator,
        SavingsAccountNumberGenerator savingsAccountNumberGenerator,
        AuditLogService auditLogService,
        NotificationService notificationService,
        EmailService emailService
    ) {
        this.userAccountDAO = userAccountDAO;
        this.memberDAO = memberDAO;
        this.savingsAccountDAO = savingsAccountDAO;
        this.memberConverter = memberConverter;
        this.passwordEncoder = passwordEncoder;
        this.membershipNumberGenerator = membershipNumberGenerator;
        this.savingsAccountNumberGenerator = savingsAccountNumberGenerator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public Member registerMember(MemberRegistrationForm form) {

        validateForm(form);

        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        String nationalId = form.getNationalId().trim();

        if (userAccountDAO.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        if (userAccountDAO.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        if (memberDAO.existsByNationalId(nationalId)) {
            throw new IllegalArgumentException(
                    "This NIN already exists"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(form.getPassword());

        UserAccount userAccount =
                memberConverter.toUserAccount(form, hashedPassword);

        UserAccount savedAccount =userAccountDAO.save(userAccount);

        String membershipNumber = membershipNumberGenerator.generate();

        Member member = memberConverter.toMember(form, savedAccount, membershipNumber);

        Member savedMember = memberDAO.save(member);

        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setMember(savedMember);
        savingsAccount.setAccountNumber(savingsAccountNumberGenerator.generate());
        savingsAccount.setBalance(BigDecimal.ZERO);
        savingsAccountDAO.save(savingsAccount);

        registerPostCommitActions(savedAccount, savedMember, membershipNumber);
        
        return savedMember;
    }

    private void registerPostCommitActions(UserAccount savedAccount, Member savedMember, String membershipNumber) {
        Runnable actions = () -> {
            try {
                auditLogService.record(savedAccount, AuditAction.MEMBER_REGISTERED, "Member", savedMember.getId(), "Membership " + membershipNumber);
            } catch (RuntimeException auditFailure) {
                logger.error("Could not record registration audit log for member {}", savedMember.getId(), auditFailure);
            }

            try {
                notificationService.notifyAdmins(com.kimwanyi.model.enums.NotificationType.SYSTEM,
                        "Member approval required",
                        savedAccount.getFirstName() + " " + savedAccount.getLastName() + " registered as " + membershipNumber + ". Review and approve the account.");
            } catch (RuntimeException notificationFailure) {
                logger.error("Could not notify administrators about member {}", savedMember.getId(), notificationFailure);
            }

            try {
                emailService.sendToMembers(List.of(savedMember.getId()),
                        "Kimwanyi SACCO registration received",
                        "Hello " + savedAccount.getFirstName() + ",\n\n"
                                + "Your application to become a member has been received. "
                                + "Membership number: " + membershipNumber + ".\n\n"
                                + "Your account is awaiting verification and approval by an admin. "
                                + "You'll be notified when approval is complete.\n\n"
                                + "Kimwanyi SACCO",
                        savedAccount);
            } catch (RuntimeException emailFailure) {
                try {
                    auditLogService.record(savedAccount, AuditAction.EMAIL_FAILED, "Member", savedMember.getId(),
                            "Registration acknowledgement could not be sent: " + emailFailure.getMessage());
                } catch (RuntimeException auditFailure) {
                    logger.error("Could not record registration email failure for member {}", savedMember.getId(), auditFailure);
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

    @Override
    @Transactional(readOnly = true)
    public MemberDto getByUserAccountId(Long userAccountId) {
        Member member = memberDAO.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        return memberConverter.toDto(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberDto> search(String keyword) {
        List<Member> members = keyword == null || keyword.isBlank()
                ? memberDAO.findAllWithUserAccount()
                : memberDAO.search(keyword.trim());
        return members.stream().map(memberConverter::toDto).toList();
    }

    @Override
    @Transactional
    public MemberDto updateProfile(Long userAccountId, MemberUpdateForm form) {
        if (form == null || form.getEmail() == null || form.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        Member member = memberDAO.findByUserAccountId(userAccountId).orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        UserAccount account = member.getUserAccount();
        String email = form.getEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(account.getEmail()) && userAccountDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        account.setEmail(email);
        member.setPhoneNumber(form.getPhoneNumber() == null || form.getPhoneNumber().isBlank() ? null : form.getPhoneNumber().trim());
        userAccountDAO.save(account);
        memberDAO.save(member);
        auditLogService.record(account, AuditAction.MEMBER_UPDATED, "Member", member.getId(), "Contact profile updated");
        return memberConverter.toDto(member);
    }

    private void validateForm(MemberRegistrationForm form) {

        if (form == null) {
            throw new IllegalArgumentException(
                    "Registration form is required"
            );
        }

        if (form.getUsername() == null ||
                form.getUsername().isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        if (form.getPassword() == null ||
                form.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        if (form.getFirstName() == null ||
                form.getFirstName().isBlank()) {
            throw new IllegalArgumentException(
                    "First name is required"
            );
        }

        if (form.getLastName() == null ||
                form.getLastName().isBlank()) {
            throw new IllegalArgumentException(
                    "Last name is required"
            );
        }

        if (form.getEmail() == null ||
                form.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (form.getNationalId() == null ||
                form.getNationalId().isBlank()) {
            throw new IllegalArgumentException(
                    "National ID is required"
            );
        }
    }
}
