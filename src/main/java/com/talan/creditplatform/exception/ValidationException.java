package com.talan.creditplatform.exception;

public class ValidationException extends AppException {
    public ValidationException(String message) {
        super(ErrorCode.CP_ERR_1001, message);
    }
}
