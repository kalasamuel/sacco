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
import com.pahappa.sacco.util.TransactionUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

//Member registration, login, and profile management
@ApplicationScoped
public class MemberService {

    private final MemberDao memberDao;
    private final AccountDao accountDao;
    private final AuditLogDao auditLogDao;
    private final LoanDao loanDao;
    private final com.pahappa.sacco.dao.TransactionDao transactionDao;

    @Inject
    public MemberService(MemberDao memberDao, AccountDao accountDao, AuditLogDao auditLogDao, LoanDao loanDao, com.pahappa.sacco.dao.TransactionDao transactionDao) {
        this.memberDao = memberDao;
        this.accountDao = accountDao;
        this.auditLogDao = auditLogDao;
        this.loanDao = loanDao;
        this.transactionDao = transactionDao;
    }

    protected MemberService() {
        this.memberDao = null;
        this.accountDao = null;
        this.auditLogDao = null;
        this.loanDao = null;
        this.transactionDao = null;
    }

    //Registers a new member AND opens their savings account in the same transaction
    public Member registerMember(String membershipNumber, String nationalId, String firstName,
                                   String lastName, String phone, String email, BigDecimal initialDeposit, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN, Role.CASHIER);

        if (memberDao.findByNationalId(nationalId).isPresent()) {
            throw new BusinessRuleViolationException("A member with this National ID already exists.");
        }
        if (memberDao.findByMembershipNumber(membershipNumber).isPresent()) {
            throw new BusinessRuleViolationException("This membership number is already in use.");
        }

        // Minimum initial deposit guard required by the brief
        BigDecimal minInitial = new BigDecimal("20000.00");
        if (initialDeposit == null || initialDeposit.compareTo(minInitial) < 0) {
            throw new BusinessRuleViolationException("Initial deposit must be at least UGX 20,000.");
        }

        return TransactionUtil.execute(em -> {
            Member member = new Member(membershipNumber, nationalId, firstName, lastName, phone, email);
            memberDao.save(member);

            Account account = new Account(member);
            // apply initial deposit to account and persist ledger row
            account.credit(initialDeposit);
            accountDao.save(account);
            member.setAccount(account);

            // persist a ledger transaction representing the initial deposit
            com.pahappa.sacco.entity.Transaction initTx = new com.pahappa.sacco.entity.Transaction(
                    account, performedBy, com.pahappa.sacco.entity.TransactionType.DEPOSIT,
                    initialDeposit, account.getBalance(), "INIT-" + UUID.randomUUID().toString().substring(0,8).toUpperCase()
            );
            transactionDao.save(initTx);

            auditLogDao.save(new AuditLog(performedBy, "MEMBER", member.getId(), "REGISTER",
                    "Registered member " + membershipNumber));

            return member;
        });
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
