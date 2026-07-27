package com.talan.creditplatform.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail, ErrorCode errorCode) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", MDC.get("traceId"));
        if (errorCode != null) {
            problemDetail.setProperty("errorCode", errorCode.name());
        }
        return problemDetail;
    }

    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof ConflictException) {
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof ValidationException) {
            status = HttpStatus.BAD_REQUEST;
        }

        ProblemDetail problem = createProblemDetail(status, ex.getErrorCode().getDescription(), ex.getMessage(), ex.getErrorCode());
        
        if (status.is4xxClientError()) {
            logger.warn("Client Error ({}): {}", ex.getErrorCode(), ex.getMessage());
        } else {
            logger.error("Server Error ({}): {}", ex.getErrorCode(), ex.getMessage(), ex);
        }
        
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ProblemDetail problem = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid payload", ErrorCode.CP_ERR_1001);
        problem.setProperty("validationErrors", errors);
        
        logger.info("Validation Failure: {}", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        
        ProblemDetail problem = createProblemDetail(HttpStatus.BAD_REQUEST, "Constraint Violation", "Invalid parameters", ErrorCode.CP_ERR_1001);
        problem.setProperty("validationErrors", errors);
        
        logger.info("Constraint Violation: {}", errors);
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        ProblemDetail problem = createProblemDetail(HttpStatus.NOT_FOUND, "Not Found", "Resource not found", ErrorCode.CP_ERR_1002);
        logger.warn("Resource Not Found: {}", ex.getResourcePath());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", ErrorCode.CP_ERR_5000);
        logger.error("Unhandled Exception: {}", ex.getMessage(), ex);
        return problem;
    }
}
