package com.talan.creditplatform.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class E164PhoneValidator implements ConstraintValidator<E164Phone, String> {
    
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Allow null/empty - use @NotNull/@NotBlank separately if required
        }
        return E164_PATTERN.matcher(value.trim()).matches();
    }
}
