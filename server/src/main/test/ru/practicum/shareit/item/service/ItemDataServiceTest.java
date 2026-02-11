package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.dto.BookingInfoDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Тестирование сервиса данных предметов")
@ExtendWith(MockitoExtension.class)
class ItemDataServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private ItemDataService itemDataService;

    private ItemDto itemDto;
    private Booking lastBooking;
    private Booking nextBooking;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Тестовый предмет")
                .ownerId(1L)
                .available(true)
                .build();

        lastBooking = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(2L)
                .status(BookingStatus.APPROVED)
                .end(LocalDateTime.now().minusDays(1))
                .build();

        nextBooking = Booking.builder()
                .id(2L)
                .itemId(1L)
                .bookerId(3L)
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().plusDays(1))
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("Тестовый комментарий")
                .authorName("Тестовый пользователь")
                .created(LocalDateTime.now())
                .build();

    }

    @Test
    @DisplayName("Обогащение предмета информацией о бронировании при совпадении владельца должно устанавливать информацию")
    void enrichItemWithBookingInfo_whenOwnerMatches_shouldSetBookingInfo() {

        Page<Booking> lastBookingsPage = new PageImpl<>(List.of(lastBooking));
        Page<Booking> nextBookingsPage = new PageImpl<>(List.of(nextBooking));

        when(bookingRepository.findLastBooking(eq(1L), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(lastBookingsPage);
        when(bookingRepository.findNextBooking(eq(1L), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(nextBookingsPage);

        itemDataService.enrichItemWithBookingInfo(itemDto, 1L);

        assertNotNull(itemDto.getLastBooking());
        assertEquals(1L, itemDto.getLastBooking().getId());
        assertEquals(2L, itemDto.getLastBooking().getBookerId());

        assertNotNull(itemDto.getNextBooking());
        assertEquals(2L, itemDto.getNextBooking().getId());
        assertEquals(3L, itemDto.getNextBooking().getBookerId());

    }

    @Test
    @DisplayName("Обогащение предмета информацией о бронировании при несовпадении владельца не должно устанавливать информацию")
    void enrichItemWithBookingInfo_whenOwnerNotMatch_shouldNotSetBookingInfo() {

        itemDataService.enrichItemWithBookingInfo(itemDto, 2L);

        assertNull(itemDto.getLastBooking());
        assertNull(itemDto.getNextBooking());
        verify(bookingRepository, never()).findLastBooking(any(), any(), any());
        verify(bookingRepository, never()).findNextBooking(any(), any(), any());

    }

    @Test
    @DisplayName("Обогащение предмета информацией о бронировании при отсутствии бронирований должно устанавливать null")
    void enrichItemWithBookingInfo_whenNoBookingsFound_shouldNotSetBookingInfo() {

        Page<Booking> emptyPage = new PageImpl<>(Collections.emptyList());

        when(bookingRepository.findLastBooking(eq(1L), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(emptyPage);
        when(bookingRepository.findNextBooking(eq(1L), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        itemDataService.enrichItemWithBookingInfo(itemDto, 1L);

        assertNull(itemDto.getLastBooking());
        assertNull(itemDto.getNextBooking());
    }

    @Test
    @DisplayName("Обогащение пустого списка предметов дополнительными данными не должно ничего делать")
    void enrichItemsWithAdditionalData_whenItemsEmpty_shouldDoNothing() {

        itemDataService.enrichItemsWithAdditionalData(Collections.emptyList(), 1L);

        verify(commentService, never()).getCommentsForItems(any());
        verify(bookingRepository, never()).findLastBookingsForMultipleItems(any(),
                any(), any());
        verify(bookingRepository, never()).findNextBookingsForMultipleItems(any(),
                any(), any());

    }

    @Test
    @DisplayName("Обогащение null списка предметов дополнительными данными не должно ничего делать")
    void enrichItemsWithAdditionalData_whenItemsNull_shouldDoNothing() {

        itemDataService.enrichItemsWithAdditionalData(null, 1L);

        verify(commentService, never()).getCommentsForItems(any());
        verify(bookingRepository, never()).findLastBookingsForMultipleItems(any(),
                any(), any());
        verify(bookingRepository, never()).findNextBookingsForMultipleItems(any(),
                any(), any());

    }

    @Test
    @DisplayName("Обогащение нескольких предметов дополнительными данными должно обогащать все предметы")
    void enrichItemsWithAdditionalData_whenMultipleItems_shouldEnrichAll() {

        ItemDto item1 = ItemDto.builder().id(1L).ownerId(1L).build();
        ItemDto item2 = ItemDto.builder().id(2L).ownerId(1L).build();
        ItemDto item3 = ItemDto.builder().id(3L).ownerId(2L).build();

        List<ItemDto> items = List.of(item1, item2, item3);

        when(commentService.getCommentsForItems(anyList()))
                .thenReturn(Map.of(
                        1L, List.of(commentDto),
                        2L, List.of(commentDto),
                        3L, List.of(commentDto)
                ));

        Booking lastBooking1 = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(2L)
                .status(BookingStatus.APPROVED)
                .end(LocalDateTime.now().minusDays(1))
                .build();

        Booking lastBooking2 = Booking.builder()
                .id(3L)
                .itemId(2L)
                .bookerId(3L)
                .status(BookingStatus.APPROVED)
                .end(LocalDateTime.now().minusDays(2))
                .build();

        Booking nextBooking1 = Booking.builder()
                .id(2L)
                .itemId(1L)
                .bookerId(3L)
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().plusDays(1))
                .build();

        Booking nextBooking2 = Booking.builder()
                .id(4L)
                .itemId(2L)
                .bookerId(4L)
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().plusDays(2))
                .build();

        when(bookingRepository.findLastBookingsForMultipleItems(anyList(), any(),
                any()))
                .thenReturn(List.of(lastBooking1, lastBooking2));
        when(bookingRepository.findNextBookingsForMultipleItems(anyList(), any(),
                any()))
                .thenReturn(List.of(nextBooking1, nextBooking2));

        itemDataService.enrichItemsWithAdditionalData(items, 1L);

        assertNotNull(item1.getLastBooking());
        assertNotNull(item1.getNextBooking());
        assertNotNull(item2.getLastBooking());
        assertNotNull(item2.getNextBooking());

        assertNull(item3.getLastBooking());
        assertNull(item3.getNextBooking());

        assertFalse(item1.getComments().isEmpty());
        assertFalse(item2.getComments().isEmpty());
        assertFalse(item3.getComments().isEmpty());

    }

    @Test
    @DisplayName("Обогащение предмета дополнительными данными должно обогащать комментариями и бронированиями")
    void enrichItemWithAdditionalData_shouldEnrichWithCommentsAndBookings() {

        when(commentService.getCommentsForItem(1L)).thenReturn(List.of(commentDto));

        Page<Booking> lastBookingsPage = new PageImpl<>(List.of(lastBooking));
        Page<Booking> nextBookingsPage = new PageImpl<>(List.of(nextBooking));

        when(bookingRepository.findLastBooking(eq(1L), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(lastBookingsPage);
        when(bookingRepository.findNextBooking(eq(1L), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(nextBookingsPage);

        itemDataService.enrichItemWithAdditionalData(itemDto, 1L);

        assertFalse(itemDto.getComments().isEmpty());
        assertEquals(1L, itemDto.getComments().getFirst().getId());
        assertNotNull(itemDto.getLastBooking());
        assertNotNull(itemDto.getNextBooking());

    }

    @Test
    @DisplayName("Загрузка последних бронирований для пустого списка предметов должна возвращать пустую мапу")
    void loadLastBookingsForItems_whenItemIdsEmpty_shouldReturnEmptyMap() {

        Map<Long, BookingInfoDto> result = itemDataService.loadLastBookingsForItems(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository, never()).findLastBookingsForMultipleItems(any(), any(),
                any());

    }

    @Test
    @DisplayName("Загрузка следующих бронирований для пустого списка предметов должна возвращать пустую мапу")
    void loadNextBookingsForItems_whenItemIdsEmpty_shouldReturnEmptyMap() {

        Map<Long, BookingInfoDto> result = itemDataService.loadNextBookingsForItems(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository, never()).findNextBookingsForMultipleItems(any(), any(),
                any());

    }

    @Test
    @DisplayName("Обогащение предмета с несуществующими бронированиями должно устанавливать null значения")
    void enrichItemWithBookingInfo_whenNoApprovedBookings_shouldSetNull() {

        Page<Booking> emptyPage = new PageImpl<>(Collections.emptyList());
        when(bookingRepository.findLastBooking(anyLong(),
                any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(bookingRepository.findNextBooking(anyLong(),
                any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        itemDataService.enrichItemWithBookingInfo(itemDto, 1L);

        assertNull(itemDto.getLastBooking());
        assertNull(itemDto.getNextBooking());
    }

    @Test
    @DisplayName("Загрузка последних бронирований для нескольких предметов должна возвращать правильные данные")
    void loadLastBookingsForItems_whenMultipleItems_shouldReturnCorrectMap() {

        List<Long> itemIds = List.of(1L, 2L, 3L);

        Booking booking1 = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(2L)
                .status(BookingStatus.APPROVED)
                .end(LocalDateTime.now().minusDays(1))
                .build();

        Booking booking2 = Booking.builder()
                .id(3L)
                .itemId(2L)
                .bookerId(3L)
                .status(BookingStatus.APPROVED)
                .end(LocalDateTime.now().minusDays(2))
                .build();

        when(bookingRepository.findLastBookingsForMultipleItems(anyList(),
                any(LocalDateTime.class), any(BookingStatus.class)))
                .thenReturn(List.of(booking1, booking2));

        Map<Long, BookingInfoDto> result = itemDataService.loadLastBookingsForItems(itemIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(1L));
        assertTrue(result.containsKey(2L));
        assertFalse(result.containsKey(3L));

        assertEquals(1L, result.get(1L).getId());
        assertEquals(2L, result.get(1L).getBookerId());
        assertEquals(3L, result.get(2L).getId());
        assertEquals(3L, result.get(2L).getBookerId());
    }

    @Test
    @DisplayName("Загрузка следующих бронирований с дубликатами должна брать первое бронирование")
    void loadNextBookingsForItems_whenDuplicateBookings_shouldTakeFirst() {

        List<Long> itemIds = List.of(1L);

        Booking booking1 = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(2L)
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().plusDays(1))
                .build();

        Booking booking2 = Booking.builder()
                .id(2L)
                .itemId(1L)
                .bookerId(3L)
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().plusDays(2))
                .build();

        when(bookingRepository.findNextBookingsForMultipleItems(anyList(),
                any(LocalDateTime.class), any(BookingStatus.class)))
                .thenReturn(List.of(booking1, booking2));

        Map<Long, BookingInfoDto> result = itemDataService.loadNextBookingsForItems(itemIds);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(1L).getId());
        assertEquals(2L, result.get(1L).getBookerId());
    }

    @Test
    @DisplayName("Обогащение предмета с null комментариями должно обрабатываться корректно")
    void enrichItemWithAdditionalData_whenCommentsNull_shouldHandleCorrectly() {

        when(commentService.getCommentsForItem(anyLong())).thenReturn(null);

        Page<Booking> emptyPage = new PageImpl<>(Collections.emptyList());
        when(bookingRepository.findLastBooking(anyLong(), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(emptyPage);
        when(bookingRepository.findNextBooking(anyLong(), any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        itemDataService.enrichItemWithAdditionalData(itemDto, 1L);

        assertNull(itemDto.getComments());
    }
}