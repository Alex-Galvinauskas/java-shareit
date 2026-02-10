package ru.practicum.shareit.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.practicum.shareit.annotation.validation.EndDateAfterStartDateValidator;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
@Documented
public @interface EndDateAfterStartDate {
    String message() default "Дата окончания должна быть после даты начала";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}