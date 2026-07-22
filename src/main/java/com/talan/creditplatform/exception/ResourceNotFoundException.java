package com.talan.creditplatform.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.CP_ERR_1002, message);
    }
}
