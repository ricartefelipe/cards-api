package com.altbank.cardsapi.application.exception;

public class BadRequestException extends AppException {
    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, 400, message);
    }
}
