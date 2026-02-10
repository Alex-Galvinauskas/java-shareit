package ru.practicum.shareit.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ItemRequestDtoTest extends ModelValidationTest<ItemRequestDto> {
    private ItemRequestDto dto;

    @BeforeEach
    public void initDto() {
        dto = ItemRequestDto.builder()
                .description("Валидное описание")
                .build();
    }

    @Test
    public void shouldNotFindViolation() {
        assertTrue(isModelValid(dto));
    }

    @Nested
    class DescriptionTest {
        @Test
        public void shouldFindViolationWhenDescriptionIsNull() {
            dto.setDescription(null);
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenDescriptionIsEmpty() {
            dto.setDescription("");
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenDescriptionIsBlank() {
            dto.setDescription("   ");
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenDescriptionIsValid() {
            dto.setDescription("Валидное описание");
            assertTrue(isModelValid(dto));
        }
    }
}