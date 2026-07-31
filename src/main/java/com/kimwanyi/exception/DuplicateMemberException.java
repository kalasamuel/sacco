package com.kimwanyi.exception;

public class DuplicateMemberException extends BusinessException {

    public DuplicateMemberException(String message) {
        super(message);
    }

    public DuplicateMemberException(String message, Throwable cause) {
        super(message, cause);
    }
}
