package com.kimwanyi.dao;

import com.kimwanyi.model.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepaymentDAO extends JpaRepository<LoanRepayment, Long> {
}
