package com.linkvault.common.exception;

public class TooManyRequestsException extends AppException {

    public TooManyRequestsException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
