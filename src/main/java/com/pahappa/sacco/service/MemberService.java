package com.pahappa.sacco.service;

import com.pahappa.sacco.dao.AccountDao;
import com.pahappa.sacco.dao.AuditLogDao;
import com.pahappa.sacco.dao.LoanDao;
import com.pahappa.sacco.dao.MemberDao;
import com.pahappa.sacco.entity.Account;
import com.pahappa.sacco.entity.AuditLog;
import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.security.RoleGuard;
import com.pahappa.sacco.service.UserService;
import com.pahappa.sacco.service.EmailService;
import com.pahappa.sacco.util.TransactionUtil;
import com.pahappa.sacco.service.MemberRegistrationResult;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

//Member registration, login, and profile management
@ApplicationScoped
public class MemberService {

    private final MemberDao memberDao;
    private final AccountDao accountDao;
    private final AuditLogDao auditLogDao;
    private final LoanDao loanDao;
    private final com.pahappa.sacco.dao.TransactionDao transactionDao;
    private final UserService userService;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    public MemberService(MemberDao memberDao, AccountDao accountDao, AuditLogDao auditLogDao, LoanDao loanDao,
                         com.pahappa.sacco.dao.TransactionDao transactionDao, UserService userService, EmailService emailService) {
        this.memberDao = memberDao;
        this.accountDao = accountDao;
        this.auditLogDao = auditLogDao;
        this.loanDao = loanDao;
        this.transactionDao = transactionDao;
        this.userService = userService;
        this.emailService = emailService;
    }

    protected MemberService() {
        this.memberDao = null;
        this.accountDao = null;
        this.auditLogDao = null;
        this.loanDao = null;
        this.transactionDao = null;
        this.userService = null;
        this.emailService = null;
    }

    //Registers a new member AND opens their savings account in the same transaction
    //also creates a member user account and sends temporary login credentials.
    public Member registerMember(String membershipNumber, String nationalId, String firstName,
                                 String lastName, String phone, String email, BigDecimal initialDeposit, User performedBy) {
        return registerMemberWithCredentials(membershipNumber, nationalId, firstName, lastName,
                phone, email, initialDeposit, performedBy, Role.MEMBER).getMember();
    }

    public Member registerMember(String membershipNumber, String nationalId, String firstName,
                                 String lastName, String phone, String email, BigDecimal initialDeposit,
                                 Role requestedRole, User performedBy) {
        return registerMemberWithCredentials(membershipNumber, nationalId, firstName, lastName,
                phone, email, initialDeposit, performedBy, requestedRole).getMember();
    }

    public MemberRegistrationResult registerMemberWithCredentials(String membershipNumber, String nationalId, String firstName,
                                                                   String lastName, String phone, String email, BigDecimal initialDeposit, User performedBy) {
        return registerMemberWithCredentials(membershipNumber, nationalId, firstName, lastName,
                phone, email, initialDeposit, performedBy, Role.MEMBER);
    }

    public MemberRegistrationResult registerMemberWithCredentials(String membershipNumber, String nationalId, String firstName,
                                                                   String lastName, String phone, String email, BigDecimal initialDeposit,
                                                                   User performedBy, Role requestedRole) {
        RoleGuard.require(performedBy, Role.ADMIN, Role.CASHIER);

        Role effectiveRole = resolveRequestedRole(performedBy, requestedRole);

        if (memberDao.findByNationalId(nationalId).isPresent()) {
            throw new BusinessRuleViolationException("A member with this National ID already exists.");
        }

        String effectiveMembershipNumber = membershipNumber;
        if (effectiveMembershipNumber == null || effectiveMembershipNumber.isBlank()) {
            effectiveMembershipNumber = generateMembershipNumber(firstName, lastName);
        }
        if (memberDao.findByMembershipNumber(effectiveMembershipNumber).isPresent()) {
            throw new BusinessRuleViolationException("This membership number is already in use.");
        }

        final String membershipNumberToUse = effectiveMembershipNumber;

        BigDecimal minInitial = new BigDecimal("20000.00");
        if (initialDeposit == null || initialDeposit.compareTo(minInitial) < 0) {
            throw new BusinessRuleViolationException("Initial deposit must be at least UGX 20,000.");
        }

        String temporaryPassword = generateTemporaryPassword();
        String generatedUsername = userService.generateUniqueUsername(firstName, lastName, membershipNumberToUse);

        MemberRegistrationResult result = TransactionUtil.execute(em -> {
            Member member = new Member(membershipNumberToUse, nationalId, firstName, lastName, phone, email);
            memberDao.save(member);

            Account account = new Account(member);
            account.credit(initialDeposit);
            accountDao.save(account);
            member.setAccount(account);

            com.pahappa.sacco.entity.Transaction initTx = new com.pahappa.sacco.entity.Transaction(
                    account, performedBy, com.pahappa.sacco.entity.TransactionType.DEPOSIT,
                    initialDeposit, account.getBalance(), "INIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            );
            transactionDao.save(initTx);

            User user = userService.createSystemUser(member, generatedUsername, temporaryPassword, effectiveRole);

            auditLogDao.save(new AuditLog(performedBy, "MEMBER", member.getId(), "REGISTER",
                    "Registered member " + membershipNumberToUse + " with username " + generatedUsername));

            return new MemberRegistrationResult(member, generatedUsername, temporaryPassword);
        });

        emailService.sendRegistrationCredentials(email, result.getUsername(), result.getTemporaryPassword(),
                result.getMember().getFullName(), result.getMember().getMembershipNumber());

        return result;
    }

    public Role resolveRequestedRole(User actingUser, Role requestedRole) {
        Role effectiveRole = requestedRole == null ? Role.MEMBER : requestedRole;
        if (actingUser != null && actingUser.getRole() == Role.ADMIN) {
            return effectiveRole;
        }
        if (effectiveRole == Role.MEMBER) {
            return Role.MEMBER;
        }
        throw new BusinessRuleViolationException("Only administrators may assign staff roles.");
    }

    public String generateMembershipNumber(String firstName, String lastName) {
        String normalized = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim()))
                .replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.isBlank()) {
            normalized = "MEM";
        }
        if (normalized.length() > 12) {
            normalized = normalized.substring(0, 12);
        }
        String prefix = "M" + LocalDate.now().getYear() + "-" + normalized;
        String candidate = prefix + "-" + String.format("%04d", RANDOM.nextInt(10000));
        while (memberDao.findByMembershipNumber(candidate).isPresent()) {
            candidate = prefix + "-" + String.format("%04d", RANDOM.nextInt(10000));
        }
        return candidate;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return password.toString();
    }

    public Member updateProfile(Long memberId, String phone, String email, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN, Role.CASHIER, Role.MEMBER);

        return TransactionUtil.execute(em -> {
            Member member = memberDao.findById(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));

            // a MEMBER-role caller may only edit their own profile — staff may edit any.
            if (performedBy.getRole() == Role.MEMBER
                    && (performedBy.getMemberProfile() == null
                        || !performedBy.getMemberProfile().getId().equals(memberId))) {
                throw new BusinessRuleViolationException("You may only update your own profile.");
            }

            member.setPhone(phone);
            member.setEmail(email);
            return member;
        });
    }

    // Deactivated, never deleted 
    public void deactivateMember(Long memberId, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN);

        TransactionUtil.executeVoid(em -> {
            Member member = memberDao.findById(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));

            List<Loan> inFlight = loanDao.findInFlightLoansForMember(memberId);
            if (!inFlight.isEmpty()) {
                throw new BusinessRuleViolationException(
                        "Cannot deactivate this member: they have an unresolved loan balance. "
                                + "The loan must be fully repaid, rejected, or otherwise closed first.");
            }

            member.setActive(false);
            auditLogDao.save(new AuditLog(performedBy, "MEMBER", member.getId(), "DEACTIVATE", null));
        });
    }

    //Read-only check the deactivation confirmation modal (UI) calls
    //BEFORE showing the confirm dialog, so the admin sees "this member
    //has an active loan and cannot be deactivated" up front rather than
    //clicking confirm and then hitting a rejected transaction. 
    public boolean hasBlockingActiveLoan(Long memberId) {
        return !loanDao.findInFlightLoansForMember(memberId).isEmpty();
    }

    public Member findById(Long memberId) {
        return memberDao.findById(memberId)
                .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));
    }

    public List<Member> search(String query) {
        if (query == null || query.isBlank()) {
            return memberDao.findAllActive();
        }
        String trimmed = query.trim();
        return memberDao.findByMembershipNumber(trimmed)
                .or(() -> memberDao.findByNationalId(trimmed))
                .map(List::of)
                .orElseGet(() -> memberDao.searchByName(trimmed));
    }

    public long getActiveMemberCount() {
        return memberDao.countActive();
    }

    public List<Member> getAllMembers() {
        return memberDao.findAll();
    }

    public boolean isNationalIdTaken(String nationalId) {
        return memberDao.findByNationalId(nationalId).isPresent();
    }

    public boolean isMembershipNumberTaken(String membershipNumber) {
        return memberDao.findByMembershipNumber(membershipNumber).isPresent();
    }
}
