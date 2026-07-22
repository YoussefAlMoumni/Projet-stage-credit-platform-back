package com.talan.creditplatform.exception;

public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(ErrorCode.CP_ERR_1003, message);
    }
}
