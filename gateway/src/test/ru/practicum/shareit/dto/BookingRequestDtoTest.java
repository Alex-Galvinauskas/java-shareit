package ru.practicum.shareit.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingRequestDtoTest extends ModelValidationTest<BookingRequestDto> {
    private BookingRequestDto dto;
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 1, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2030, 1, 2, 0, 0);

    @BeforeEach
    public void initDto() {
        dto = new BookingRequestDto();
        dto.setItemId(1L);
        dto.setStart(start);
        dto.setEnd(end);
    }

    @Test
    public void shouldNotFindViolation() {
        assertTrue(isModelValid(dto));
    }

    @Nested
    class ItemIdTest {
        @Test
        public void shouldFindViolationWhenItemIdIsNull() {
            dto.setItemId(null);
            assertFalse(isModelValid(dto));
        }
    }

    @Nested
    class StartTest {
        @Test
        public void shouldFindViolationWhenStartInPast() {
            dto.setStart(LocalDateTime.now().minusDays(1));
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenStartIsNull() {
            dto.setStart(null);
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenStartIsPresent() {
            dto.setStart(LocalDateTime.now());
            assertTrue(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenStartIsFuture() {
            dto.setStart(LocalDateTime.now().plusDays(1));
            assertTrue(isModelValid(dto));
        }
    }

    @Nested
    class EndTest {
        @Test
        public void shouldFindViolationWhenEndInPast() {
            dto.setEnd(LocalDateTime.now().minusDays(1));
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenEndIsNull() {
            dto.setEnd(null);
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenEndNotFuture() {
            dto.setEnd(LocalDateTime.now());
            assertFalse(isModelValid(dto));
        }
    }

    @Test
    public void shouldFindViolationWhenEndBeforeStart() {
        dto.setStart(LocalDateTime.of(2030, 1, 2, 0, 0));
        dto.setEnd(LocalDateTime.of(2030, 1, 1, 0, 0));
        assertFalse(isModelValid(dto));
    }
}