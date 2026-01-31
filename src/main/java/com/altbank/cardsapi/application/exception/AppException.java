package com.altbank.cardsapi.application.exception;

public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int httpStatus;

    public AppException(ErrorCode errorCode, int httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
