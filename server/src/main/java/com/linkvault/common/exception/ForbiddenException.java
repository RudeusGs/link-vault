package com.linkvault.common.exception;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(ErrorCode.AUTH_ACCESS_DENIED, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
