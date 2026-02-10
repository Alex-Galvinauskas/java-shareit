package ru.practicum.shareit.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.practicum.shareit.annotation.EndDateAfterStartDate;
import ru.practicum.shareit.dto.BookingRequestDto;

public class EndDateAfterStartDateValidator
        implements ConstraintValidator<EndDateAfterStartDate, BookingRequestDto> {

    @Override
    public void initialize(EndDateAfterStartDate constraintAnnotation) {
    }

    @Override
    public boolean isValid(BookingRequestDto dto, ConstraintValidatorContext context) {
        if (dto.getStart() == null || dto.getEnd() == null) {
            return true;
        }

        return dto.getEnd().isAfter(dto.getStart());
    }
}