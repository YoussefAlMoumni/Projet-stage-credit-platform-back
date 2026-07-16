package com.talan.creditplatform.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = E164PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface E164Phone {
    String message() default "Phone number must be in E.164 format (e.g., +1234567890)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
