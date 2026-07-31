package com.kimwanyi.exception;

public class LoanNotEligibleException extends BusinessException {

    public LoanNotEligibleException(String message) {
        super(message);
    }

    public LoanNotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
