package com.altbank.cardsapi.application.exception;

public class ConflictException extends AppException {
    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, 409, message);
    }
}
