package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.Transaction;
import com.pahappa.sacco.entity.TransactionType;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TransactionDao extends GenericDao<Transaction, Long> {

    public TransactionDao() {
        super(Transaction.class);
    }

    public List<Transaction> findByAccountOrderedDesc(Long accountId) {
        TypedQuery<Transaction> query = em().createQuery(
                "SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
                "ORDER BY t.createdAt DESC", Transaction.class);
        query.setParameter("accountId", accountId);
        return query.getResultList();
    }

    public List<Transaction> findByAccountAndDateRange(Long accountId, LocalDateTime from, LocalDateTime to) {
        TypedQuery<Transaction> query = em().createQuery(
                "SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
                "AND t.createdAt BETWEEN :from AND :to ORDER BY t.createdAt ASC", Transaction.class);
        query.setParameter("accountId", accountId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    public BigDecimal sumByTypeAndDateRange(TransactionType type, LocalDateTime from, LocalDateTime to) {
        TypedQuery<BigDecimal> query = em().createQuery(
                "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                "WHERE t.type = :type AND t.createdAt BETWEEN :from AND :to", BigDecimal.class);
        query.setParameter("type", type);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getSingleResult();
    }

    public BigDecimal totalSavingsHeld() {
        TypedQuery<BigDecimal> query = em().createQuery(
                "SELECT COALESCE(SUM(a.balance), 0) FROM Account a", BigDecimal.class);
        return query.getSingleResult();
    }
}
