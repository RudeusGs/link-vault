package com.linkvault.common.exception;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
