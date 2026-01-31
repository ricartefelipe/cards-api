package com.altbank.cardsapi.application.exception;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, 401, message);
    }
}
