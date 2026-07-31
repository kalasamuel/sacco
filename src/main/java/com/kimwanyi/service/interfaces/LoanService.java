package com.kimwanyi.service.interfaces;

import java.util.List;

import com.kimwanyi.model.Loan;
import com.kimwanyi.model.dto.forms.LoanApplicationForm;
import com.kimwanyi.model.dto.forms.LoanDecisionForm;
import com.kimwanyi.model.dto.forms.LoanRepaymentForm;

public interface LoanService {
    
    Loan applyLoan(LoanApplicationForm form);
    
    Loan decideLoan(LoanDecisionForm form, Long adminUserId);
    
    void repayLoan(LoanRepaymentForm form);
    
    List<Loan> getPendingLoans();
    
    List<Loan> getLoansByMember(Long memberId);
    
    Loan getLoanById(Long loanId);
    int markOverdueLoans();
}
