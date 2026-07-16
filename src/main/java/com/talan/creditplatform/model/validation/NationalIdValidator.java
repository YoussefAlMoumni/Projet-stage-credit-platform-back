package com.talan.creditplatform.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NationalIdValidator implements ConstraintValidator<NationalId, String> {
    
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{5,20}$");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false; // National ID should not be null/blank
        }
        return NATIONAL_ID_PATTERN.matcher(value.trim()).matches();
    }
}
