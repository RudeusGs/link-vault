package com.linkvault.common.exception;

public class StorageException extends AppException {

    public StorageException(String message) {
        super(ErrorCode.STORAGE_UPLOAD_FAILED, message);
    }

    public StorageException(String message, Throwable cause) {
        super(ErrorCode.STORAGE_UPLOAD_FAILED, message, cause);
    }
}
