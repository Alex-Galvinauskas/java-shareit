package ru.practicum.shareit.annotation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.shareit.annotation.EndDateAfterStartDate;
import ru.practicum.shareit.dto.BookingRequestDto;

@Slf4j
public class EndDateAfterStartDateValidator
        implements ConstraintValidator<EndDateAfterStartDate, BookingRequestDto> {

    @Override
    public void initialize(EndDateAfterStartDate constraintAnnotation) {
        log.debug("Инициализация валидатора EndDateAfterStartDateValidator");
    }

    @Override
    public boolean isValid(BookingRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            log.warn("BookingRequestDto не может быть null");
            return true;
        }

        if (dto.getStart() == null || dto.getEnd() == null) {
            log.debug("Начальная или конечная дата не указаны, пропускаем валидацию. start={}, end={}",
                    dto.getStart(), dto.getEnd());
            return true;
        }

        boolean isValid = dto.getEnd().isAfter(dto.getStart());

        if (!isValid) {
            log.warn("Валидация не пройдена: конечная дата должна быть после начальной. start={}, end={}",
                    dto.getStart(), dto.getEnd());
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Дата окончания " + dto.getEnd() + " должна быть после даты начала " + dto.getStart()
            ).addConstraintViolation();
        } else {
            log.debug("Валидация пройдена успешно: end={} после start={}",
                    dto.getEnd(), dto.getStart());
        }

        return isValid;
    }
}