package com.altbank.cardsapi.application.exception;

public class NotFoundException extends AppException {
    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, 404, message);
    }
}
