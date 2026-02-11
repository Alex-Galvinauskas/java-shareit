package ru.practicum.shareit.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserDtoTest extends ModelValidationTest<UserDto> {
    private UserDto dto;

    @BeforeEach
    public void initDto() {
        dto = UserDto.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .build();
    }

    @Nested
    class IdTest {
        @Test
        public void shouldFindViolationWhenIdIsNotNullOnCreate() {
            dto.setId(1L);
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldNotFindViolationWhenIdIsNullOnCreate() {
            dto.setId(null);
            assertTrue(isModelValidForCreate(dto));
        }

        @Test
        public void shouldNotFindViolationWhenIdIsNotNullOnUpdate() {
            dto.setId(1L);
            assertTrue(isModelValidForUpdate(dto));
        }
    }

    @Nested
    class NameTest {
        @Test
        public void shouldFindViolationWhenNameIsNullOnCreate() {
            dto.setName(null);
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldFindViolationWhenNameIsEmptyOnCreate() {
            dto.setName("");
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldFindViolationWhenNameIsBlankOnCreate() {
            dto.setName("   ");
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldNotFindViolationWhenNameIsNullOnUpdate() {
            dto.setName(null);
            assertTrue(isModelValidForUpdate(dto));
        }
    }

    @Nested
    class EmailTest {
        @Test
        public void shouldFindViolationWhenEmailIsNullOnCreate() {
            dto.setEmail(null);
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldFindViolationWhenEmailIsEmptyOnCreate() {
            dto.setEmail("");
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldFindViolationWhenEmailIsInvalidOnCreate() {
            dto.setEmail("invalid-email");
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldFindViolationWhenEmailIsBlankOnCreate() {
            dto.setEmail("   ");
            assertFalse(isModelValidForCreate(dto));
        }

        @Test
        public void shouldNotFindViolationWhenEmailIsNullOnUpdate() {
            dto.setEmail(null);
            assertTrue(isModelValidForUpdate(dto));
        }
    }
}