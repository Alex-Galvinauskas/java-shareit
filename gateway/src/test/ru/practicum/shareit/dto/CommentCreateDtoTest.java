package ru.practicum.shareit.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommentCreateDtoTest extends ModelValidationTest<CommentCreateDto> {
    private CommentCreateDto dto;

    @BeforeEach
    public void initDto() {
        dto = CommentCreateDto.builder()
                .text("Excellent item, worked perfectly!")
                .build();
    }

    @Test
    public void shouldNotFindViolation() {
        assertTrue(isModelValid(dto));
    }

    @Nested
    class TextTest {
        @Test
        public void shouldFindViolationWhenTextIsNull() {
            dto.setText(null);
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenTextIsEmpty() {
            dto.setText("");
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenTextIsBlank() {
            dto.setText("   ");
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenTextIsValid() {
            dto.setText("This is a valid comment");
            assertTrue(isModelValid(dto));
        }
    }
}