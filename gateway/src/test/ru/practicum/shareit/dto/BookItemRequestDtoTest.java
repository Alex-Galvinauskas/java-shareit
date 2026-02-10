package ru.practicum.shareit.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookItemRequestDtoTest extends ModelValidationTest<BookItemRequestDto> {
    private BookItemRequestDto dto;

    @BeforeEach
    public void initDto() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        dto = new BookItemRequestDto(1L, start, end);
    }

    @Test
    public void shouldNotFindViolation() {
        assertTrue(isModelValid(dto));
    }

    @Nested
    class StartTest {
        @Test
        public void shouldFindViolationWhenStartInPast() {
            LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
            LocalDateTime futureEnd = LocalDateTime.now().plusDays(1);
            BookItemRequestDto invalidDto = new BookItemRequestDto(1L, pastStart, futureEnd);
            assertFalse(isModelValid(invalidDto));
        }

        @Test
        public void shouldNotFindViolationWhenStartIsPresent() {
            LocalDateTime presentStart = LocalDateTime.now().plusSeconds(1);
            LocalDateTime futureEnd = LocalDateTime.now().plusDays(1);
            BookItemRequestDto validDto = new BookItemRequestDto(1L, presentStart, futureEnd);
            assertTrue(isModelValid(validDto));
        }

        @Test
        public void shouldNotFindViolationWhenStartIsFuture() {
            LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
            LocalDateTime futureEnd = LocalDateTime.now().plusDays(2);
            BookItemRequestDto validDto = new BookItemRequestDto(1L, futureStart, futureEnd);
            assertTrue(isModelValid(validDto));
        }
    }

    @Nested
    class EndTest {
        @Test
        public void shouldFindViolationWhenEndInPast() {
            LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
            LocalDateTime pastEnd = LocalDateTime.now().minusDays(1);
            BookItemRequestDto invalidDto = new BookItemRequestDto(1L, futureStart, pastEnd);
            assertFalse(isModelValid(invalidDto));
        }

        @Test
        public void shouldFindViolationWhenEndNotFuture() {
            LocalDateTime futureStart = LocalDateTime.now().plusDays(2);
            LocalDateTime presentEnd = LocalDateTime.now();
            BookItemRequestDto invalidDto = new BookItemRequestDto(1L, futureStart, presentEnd);
            assertFalse(isModelValid(invalidDto));
        }

        @Test
        public void shouldFindViolationWhenEndIsSameAsNow() {
            LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
            LocalDateTime nowEnd = LocalDateTime.now();
            BookItemRequestDto invalidDto = new BookItemRequestDto(1L, futureStart, nowEnd);
            assertFalse(isModelValid(invalidDto));
        }

        @Test
        public void shouldNotFindViolationWhenEndIsFuture() {
            LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
            LocalDateTime futureEnd = LocalDateTime.now().plusDays(2);
            BookItemRequestDto validDto = new BookItemRequestDto(1L, futureStart, futureEnd);
            assertTrue(isModelValid(validDto));
        }

        @Test
        public void shouldNotFindViolationWhenEndIsFutureByOneSecond() {
            LocalDateTime futureStart = LocalDateTime.now().plusSeconds(1);
            LocalDateTime futureEnd = LocalDateTime.now().plusSeconds(2);
            BookItemRequestDto validDto = new BookItemRequestDto(1L, futureStart, futureEnd);
            assertTrue(isModelValid(validDto));
        }
    }

    @Test
    public void shouldFindViolationWhenStartIsExactlyNow() {
        LocalDateTime nowStart = LocalDateTime.now();
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(1);
        BookItemRequestDto dto = new BookItemRequestDto(1L, nowStart, futureEnd);

        System.out.println("Testing with start: " + nowStart);
    }

    @Test
    public void shouldHandleNullValues() {
        BookItemRequestDto nullDto = new BookItemRequestDto(1L, null, null);
    }
}