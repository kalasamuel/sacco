package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.Loan;
import com.pahappa.sacco.entity.LoanStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class LoanDao extends GenericDao<Loan, Long> {

    public LoanDao() {
        super(Loan.class);
    }

    public List<Loan> findInFlightLoansForMember(Long memberId) {
        TypedQuery<Loan> query = em().createQuery(
                "SELECT l FROM Loan l WHERE l.member.id = :memberId " +
                "AND l.status NOT IN (com.pahappa.sacco.entity.LoanStatus.REJECTED, " +
                "                     com.pahappa.sacco.entity.LoanStatus.CLOSED)",
                Loan.class);
        query.setParameter("memberId", memberId);
        return query.getResultList();
    }

    public List<Loan> findActiveOrOverdueLoansForMember(Long memberId) {
        TypedQuery<Loan> query = em().createQuery(
                "SELECT l FROM Loan l WHERE l.member.id = :memberId " +
                "AND l.status IN (com.pahappa.sacco.entity.LoanStatus.ACTIVE, " +
                "                 com.pahappa.sacco.entity.LoanStatus.OVERDUE)",
                Loan.class);
        query.setParameter("memberId", memberId);
        return query.getResultList();
    }

    public List<Loan> findByStatus(LoanStatus status) {
        TypedQuery<Loan> query = em().createQuery(
                "SELECT l FROM Loan l WHERE l.status = :status ORDER BY l.appliedAt", Loan.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<Loan> findOverdue(LocalDate asOfDate) {
        TypedQuery<Loan> query = em().createQuery(
                "SELECT l FROM Loan l WHERE l.status = com.pahappa.sacco.entity.LoanStatus.ACTIVE " +
                "AND l.dueDate < :asOfDate ORDER BY l.dueDate", Loan.class);
        query.setParameter("asOfDate", asOfDate);
        return query.getResultList();
    }

    public List<Loan> findByMember(Long memberId) {
        TypedQuery<Loan> query = em().createQuery(
                "SELECT l FROM Loan l WHERE l.member.id = :memberId ORDER BY l.appliedAt DESC", Loan.class);
        query.setParameter("memberId", memberId);
        return query.getResultList();
    }

    public long countByStatus(LoanStatus status) {
        TypedQuery<Long> query = em().createQuery(
                "SELECT COUNT(l) FROM Loan l WHERE l.status = :status", Long.class);
        query.setParameter("status", status);
        return query.getSingleResult();
    }
}
