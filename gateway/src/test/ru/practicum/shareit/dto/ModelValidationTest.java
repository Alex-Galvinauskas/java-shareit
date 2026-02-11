package ru.practicum.shareit.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public class ModelValidationTest<T> {
    protected static Validator validator;
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    public static void setUpValidator() {
        try {
            validatorFactory = Validation.buildDefaultValidatorFactory();
            validator = validatorFactory.getValidator();
        } catch (Exception e) {
            log.error("Ошибка создания валидатора: ", e);
            throw new RuntimeException("Ошибка инициализации валидатора: ", e);
        }
    }

    @AfterAll
    public static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    protected boolean isModelValid(T model) {
        return validator.validate(model).isEmpty();
    }

    protected boolean isModelValidForCreate(T model) {
        return validator.validate(model, UserDto.OnCreate.class).isEmpty();
    }

    protected boolean isModelValidForUpdate(T model) {
        return validator.validate(model, UserDto.OnUpdate.class).isEmpty();
    }
}