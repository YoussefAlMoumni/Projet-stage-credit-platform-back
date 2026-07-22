package com.talan.creditplatform.exception;

public enum ErrorCode {
    CP_ERR_1001("Validation Failed"),
    CP_ERR_1002("Resource Not Found"),
    CP_ERR_1003("Conflict / Duplicate Resource"),
    CP_ERR_1004("Unauthorized Access"),
    CP_ERR_5000("Internal Server Error"),
    CP_ERR_5001("External Service Timeout");

    private final String description;

    ErrorCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
