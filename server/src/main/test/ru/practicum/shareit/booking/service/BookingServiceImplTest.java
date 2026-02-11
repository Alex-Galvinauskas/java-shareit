package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.InvalidDateException;
import ru.practicum.shareit.exception.ItemNotAvailableException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Тестирование сервиса бронирований")
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private UserDto userDto;
    private Item item;
    private BookingRequestDto bookingRequestDto;
    private Booking booking;
    private BookingResponseDto bookingResponseDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Тестовый пользователь")
                .email("test@email.com")
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .name("Тестовый пользователь")
                .email("test@email.com")
                .build();

        User owner = User.builder()
                .id(2L)
                .name("Владелец")
                .email("owner@email.com")
                .build();

        item = Item.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(2L)
                .build();

        bookingRequestDto = BookingRequestDto.builder()
                .itemId(1L)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();

        booking = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(1L)
                .ownerId(2L)
                .status(BookingStatus.WAITING)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .created(LocalDateTime.now())
                .build();

        bookingResponseDto = BookingResponseDto.builder()
                .id(1L)
                .status(BookingStatus.WAITING)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Создание бронирования должно успешно возвращать DTO")
    void create_whenValidRequest_shouldReturnBookingResponseDto() {
        when(userService.getById(1L)).thenReturn(userDto);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsOverlappingBooking(eq(1L),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(bookingMapper.toEntity(bookingRequestDto)).thenReturn(booking);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        BookingResponseDto result = bookingService.create(1L, bookingRequestDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(BookingStatus.WAITING, result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Создание бронирования для своей вещи должно выбрасывать исключение")
    void create_whenBookingOwnItem_shouldThrowAccessDeniedException() {
        item.setOwnerId(1L); // Тот же ID, что и у пользователя

        when(userService.getById(1L)).thenReturn(userDto);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> bookingService.create(1L, bookingRequestDto));

        assertTrue(exception.getMessage().contains("Владелец не может бронировать свою вещь"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Создание бронирования для недоступной вещи должно выбрасывать исключение")
    void create_whenItemNotAvailable_shouldThrowItemNotAvailableException() {
        item.setAvailable(false);

        when(userService.getById(1L)).thenReturn(userDto);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemNotAvailableException exception = assertThrows(ItemNotAvailableException.class,
                () -> bookingService.create(1L, bookingRequestDto));

        assertTrue(exception.getMessage().contains("Вещь недоступна для бронирования"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Создание бронирования с пересечением дат должно выбрасывать исключение")
    void create_whenOverlappingBooking_shouldThrowItemNotAvailableException() {
        when(userService.getById(1L)).thenReturn(userDto);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsOverlappingBooking(eq(1L),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        ItemNotAvailableException exception = assertThrows(ItemNotAvailableException.class,
                () -> bookingService.create(1L, bookingRequestDto));

        assertTrue(exception.getMessage().contains("Вещь уже забронирована на указанные даты"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Подтверждение бронирования владельцем должно обновить статус на APPROVED")
    void approve_whenOwnerApproves_shouldReturnApprovedBooking() {
        booking.setStatus(BookingStatus.WAITING);

        Booking approvedBooking = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(1L)
                .ownerId(2L)
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .created(LocalDateTime.now())
                .build();

        BookingResponseDto approvedResponse = BookingResponseDto.builder()
                .id(1L)
                .status(BookingStatus.APPROVED)
                .build();

        when(userService.getById(2L)).thenReturn(UserDto.builder().id(2L).build());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(approvedBooking);
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(approvedResponse);

        BookingResponseDto result = bookingService.approve(2L, 1L, true);

        assertNotNull(result);
        assertEquals(BookingStatus.APPROVED, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Отклонение бронирования владельцем должно обновить статус на REJECTED")
    void approve_whenOwnerRejects_shouldReturnRejectedBooking() {
        booking.setStatus(BookingStatus.WAITING);

        Booking rejectedBooking = Booking.builder()
                .id(1L)
                .itemId(1L)
                .bookerId(1L)
                .ownerId(2L)
                .status(BookingStatus.REJECTED)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .created(LocalDateTime.now())
                .build();

        BookingResponseDto rejectedResponse = BookingResponseDto.builder()
                .id(1L)
                .status(BookingStatus.REJECTED)
                .build();

        when(userService.getById(2L)).thenReturn(UserDto.builder().id(2L).build());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(rejectedBooking);
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(rejectedResponse);

        BookingResponseDto result = bookingService.approve(2L, 1L, false);

        assertNotNull(result);
        assertEquals(BookingStatus.REJECTED, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Подтверждение бронирования не владельцем должно выбрасывать исключение")
    void approve_whenNotOwner_shouldThrowAccessDeniedException() {
        when(userService.getById(3L)).thenReturn(UserDto.builder().id(3L).build());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> bookingService.approve(3L, 1L, true));

        assertTrue(exception.getMessage().contains("Только владелец вещи может подтверждать бронирование"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Подтверждение уже обработанного бронирования должно выбрасывать исключение")
    void approve_whenAlreadyProcessed_shouldThrowAccessDeniedException() {
        booking.setStatus(BookingStatus.APPROVED);
        when(userService.getById(2L)).thenReturn(UserDto.builder().id(2L).build());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> bookingService.approve(2L, 1L, true));

        assertTrue(exception.getMessage().contains("Бронирование уже обработано"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение бронирования по ID должен возвращать DTO")
    void getById_whenBookerRequests_shouldReturnBookingResponseDto() {
        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        BookingResponseDto result = bookingService.getById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Получение бронирования по ID владельцем должно возвращать DTO")
    void getById_whenOwnerRequests_shouldReturnBookingResponseDto() {
        when(userService.getById(2L)).thenReturn(UserDto.builder().id(2L).build());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        BookingResponseDto result = bookingService.getById(2L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Получение бронирования не авторизованным пользователем должно выбрасывать исключение")
    void getById_whenUnauthorizedUser_shouldThrowAccessDeniedException() {
        when(userService.getById(3L)).thenReturn(UserDto.builder().id(3L).build());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> bookingService.getById(3L, 1L));

        assertTrue(exception.getMessage().contains("Доступ к бронирования запрещен"));
    }

    @Test
    @DisplayName("Получение бронирований пользователя с состоянием ALL должно возвращать список")
    void getUserBookings_whenStateAll_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findByBookerId(eq(1L),
                any(PageRequest.class))).thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getUserBookings(1L,
                BookingState.ALL, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований пользователя с состоянием FUTURE должно возвращать список")
    void getUserBookings_whenStateFuture_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findByBookerIdAndStartGreaterThan(eq(1L),
                any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getUserBookings(1L,
                BookingState.FUTURE, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований пользователя с состоянием CURRENT должно возвращать список")
    void getUserBookings_whenStateCurrent_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                eq(1L), any(LocalDateTime.class),
                any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getUserBookings(1L,
                BookingState.CURRENT, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований пользователя с состоянием PAST должно возвращать список")
    void getUserBookings_whenStatePast_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findByBookerIdAndEndLessThan(
                eq(1L), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getUserBookings(1L,
                BookingState.PAST, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований пользователя с состоянием WAITING должно возвращать список")
    void getUserBookings_whenStateWaiting_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findByBookerIdAndStatus(eq(1L),
                eq(BookingStatus.WAITING), any(PageRequest.class)))
                .thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getUserBookings(1L,
                BookingState.WAITING, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований пользователя с состоянием REJECTED должно возвращать список")
    void getUserBookings_whenStateRejected_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(1L)).thenReturn(userDto);
        when(bookingRepository.findByBookerIdAndStatus(eq(1L),
                eq(BookingStatus.REJECTED), any(PageRequest.class)))
                .thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getUserBookings(1L,
                BookingState.REJECTED, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований владельца с состоянием ALL должно возвращать список")
    void getOwnerBookings_whenStateAll_shouldReturnList() {
        List<Booking> bookings = List.of(booking);
        Page<Booking> page = new PageImpl<>(bookings);

        when(userService.getById(2L)).thenReturn(UserDto.builder().id(2L).build());
        when(bookingRepository.findByOwnerId(eq(2L),
                any(PageRequest.class))).thenReturn(page);
        when(itemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(userService.getUsersByIds(anyList())).thenReturn(List.of(user));
        when(bookingMapper.toResponseDto(any(Booking.class),
                any(Item.class), any(User.class)))
                .thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getOwnerBookings(2L,
                BookingState.ALL, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Получение бронирований владельца с невалидными параметрами пагинации должно выбрасывать исключение")
    void getUserBookings_whenInvalidPagination_shouldThrowException() {
        when(userService.getById(1L)).thenReturn(userDto);

        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getUserBookings(1L,
                        BookingState.ALL, -1, 10));
        assertTrue(exception1.getMessage().contains("отрицательным"));

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getUserBookings(1L,
                        BookingState.ALL, 0, 0));
        assertTrue(exception2.getMessage().contains("больше 0"));
    }


    @Test
    @DisplayName("Создание бронирования с датой начала в прошлом должно выбрасывать исключение")
    void create_whenStartInPast_shouldThrowInvalidDateException() {
        BookingRequestDto invalidRequest = BookingRequestDto.builder()
                .itemId(1L)
                .start(LocalDateTime.now().minusHours(1))
                .end(LocalDateTime.now().plusHours(1))
                .build();

        when(userService.getById(1L)).thenReturn(userDto);

        InvalidDateException exception = assertThrows(InvalidDateException.class,
                () -> bookingService.create(1L, invalidRequest));

        assertTrue(exception.getMessage().contains("Дата начала должна быть в будущем или настоящем"));
    }

    @Test
    @DisplayName("Создание бронирования с датой окончания раньше начала должно выбрасывать исключение")
    void create_whenEndBeforeStart_shouldThrowInvalidDateException() {
        BookingRequestDto invalidRequest = BookingRequestDto.builder()
                .itemId(1L)
                .start(LocalDateTime.now().plusHours(2))
                .end(LocalDateTime.now().plusHours(1))
                .build();

        when(userService.getById(1L)).thenReturn(userDto);

        InvalidDateException exception = assertThrows(InvalidDateException.class,
                () -> bookingService.create(1L, invalidRequest));

        assertTrue(exception.getMessage().contains("Дата окончания должна быть позже даты начала"));
    }

    @Test
    @DisplayName("Создание бронирования с одинаковыми датами должно выбрасывать исключение")
    void create_whenStartEqualsEnd_shouldThrowInvalidDateException() {
        LocalDateTime sameTime = LocalDateTime.now().plusHours(1);
        BookingRequestDto invalidRequest = BookingRequestDto.builder()
                .itemId(1L)
                .start(sameTime)
                .end(sameTime)
                .build();

        when(userService.getById(1L)).thenReturn(userDto);

        InvalidDateException exception = assertThrows(InvalidDateException.class,
                () -> bookingService.create(1L, invalidRequest));

        assertTrue(exception.getMessage().contains("Даты начала и окончания не могут совпадать"));
    }

    @Test
    @DisplayName("Создание бронирования для несуществующего пользователя должно выбрасывать исключение")
    void create_whenUserNotFound_shouldThrowNotFoundException() {
        when(userService.getById(999L))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.create(999L, bookingRequestDto));

        assertTrue(exception.getMessage().contains("Пользователь"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Создание бронирования для несуществующей вещи должно выбрасывать исключение")
    void create_whenItemNotFound_shouldThrowNotFoundException() {
        when(userService.getById(1L)).thenReturn(userDto);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.create(1L, bookingRequestDto));

        assertTrue(exception.getMessage().contains("Вещь с id=1 не найдена"));
        verify(bookingRepository, never()).save(any());
    }
}