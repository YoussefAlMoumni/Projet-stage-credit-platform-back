package com.talan.creditplatform.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NationalIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NationalId {
    String message() default "National ID must contain only alphanumeric characters and hyphens, 5-20 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
