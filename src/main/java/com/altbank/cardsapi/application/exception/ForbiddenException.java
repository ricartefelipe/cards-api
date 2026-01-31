package com.altbank.cardsapi.application.exception;

public class ForbiddenException extends AppException {
    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, 403, message);
    }
}
