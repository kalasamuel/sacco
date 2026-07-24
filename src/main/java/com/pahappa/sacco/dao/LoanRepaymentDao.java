package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.LoanRepayment;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import java.util.List;

@ApplicationScoped
public class LoanRepaymentDao extends GenericDao<LoanRepayment, Long> {

    public LoanRepaymentDao() {
        super(LoanRepayment.class);
    }

    public List<LoanRepayment> findByLoanOrderedDesc(Long loanId) {
        TypedQuery<LoanRepayment> query = em().createQuery(
                "SELECT r FROM LoanRepayment r WHERE r.loan.id = :loanId " +
                "ORDER BY r.createdAt DESC", LoanRepayment.class);
        query.setParameter("loanId", loanId);
        return query.getResultList();
    }
}
