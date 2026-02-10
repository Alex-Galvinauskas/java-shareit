package ru.practicum.shareit.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ItemDtoTest extends ModelValidationTest<ItemDto> {
    private ItemDto dto;

    @BeforeEach
    public void initDto() {
        dto = ItemDto.builder()
                .id(1L)
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .ownerId(1L)
                .requestId(1L)
                .lastBooking(BookingInfoDto.builder().id(1L).bookerId(1L).build())
                .nextBooking(BookingInfoDto.builder().id(2L).bookerId(2L).build())
                .comments(List.of(
                        CommentDto.builder()
                                .id(1L)
                                .text("Great item!")
                                .authorName("User1")
                                .created(LocalDateTime.now())
                                .build()
                ))
                .build();
    }

    @Test
    public void shouldNotFindViolation() {
        assertTrue(isModelValid(dto));
    }

    @Nested
    class NameTest {
        @Test
        public void shouldFindViolationWhenNameIsNull() {
            dto.setName(null);
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenNameIsEmpty() {
            dto.setName("");
            assertFalse(isModelValid(dto));
        }

        @Test
        public void shouldFindViolationWhenNameIsBlank() {
            dto.setName("   ");
            assertFalse(isModelValid(dto));
        }
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
    }

    @Nested
    class AvailableTest {
        @Test
        public void shouldFindViolationWhenAvailableIsNull() {
            dto.setAvailable(null);
            assertFalse(isModelValid(dto));
        }
    }

    @Nested
    class IdsTest {
        @Test
        public void shouldNotFindViolationWhenIdsAreNull() {
            dto.setId(null);
            dto.setOwnerId(null);
            dto.setRequestId(null);
            assertTrue(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenIdIsZero() {
            dto.setId(0L);
            assertTrue(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenOwnerIdIsZero() {
            dto.setOwnerId(0L);
            assertTrue(isModelValid(dto));
        }

        @Test
        public void shouldNotFindViolationWhenRequestIdIsZero() {
            dto.setRequestId(0L);
            assertTrue(isModelValid(dto));
        }
    }
}