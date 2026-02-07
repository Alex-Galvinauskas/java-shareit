package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponseDto create(Long userId, BookingRequestDto bookingRequestDto) {
        log.info("Создание нового бронирования для пользователя с ID={}", userId);

        userService.getById(userId);
        validateBookingRequest(bookingRequestDto);

        Item item = getItemForBooking(bookingRequestDto.getItemId());
        validateItemForBooking(item, userId);

        checkForOverlappingBookings(item.getId(), bookingRequestDto.getStart(), bookingRequestDto.getEnd());

        Booking booking = createBookingEntity(bookingRequestDto, item.getId(), userId, item.getOwnerId());
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Бронирование успешно создано с ID={}", savedBooking.getId());
        return mapToResponseDto(savedBooking);
    }

    @Transactional
    public BookingResponseDto approve(Long userId, Long bookingId, Boolean approved) {
        log.info("Подтверждение/отклонение бронирования с ID={} пользователем с ID={}, approved={}",
                bookingId, userId, approved);

        try {
            userService.getById(userId);
        } catch (NotFoundException e) {
            throw new AccessDeniedException("Пользователь не найден");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AccessDeniedException("Бронирование не найдено"));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new AccessDeniedException("Вещь не найдена"));

        if (!item.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Только владелец вещи может подтверждать бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new AccessDeniedException("Бронирование уже обработано");
        }

        BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        booking.setStatus(newStatus);

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Статус бронирования с ID={} успешно обновлен на: {}", bookingId, newStatus);

        return mapToResponseDto(updatedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getById(Long userId, Long bookingId) {
        log.debug("Получение бронирования по ID={} пользователем с ID={}", bookingId, userId);

        userService.getById(userId);

        Booking booking = getBookingById(bookingId);
        validateBookingAccess(booking, userId);

        return mapToResponseDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(Long userId, BookingState state, int from, int size) {
        log.info("Получение бронирований пользователя с ID={}, состояние={}, from={}, size={}",
                userId, state, from, size);

        userService.getById(userId);
        validatePaginationParams(from, size);

        Pageable pageable = PageRequest.of(from / size, size,
                Sort.by("start").descending());
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBookerId(userId, pageable).getContent();
            case CURRENT -> bookingRepository.findByBookerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    userId, now, now, pageable).getContent();
            case PAST -> bookingRepository.findByBookerIdAndEndLessThan(userId,
                    now, pageable).getContent();
            case FUTURE -> bookingRepository.findByBookerIdAndStartGreaterThan(userId,
                    now, pageable).getContent();
            case WAITING -> bookingRepository.findByBookerIdAndStatus(userId,
                    BookingStatus.WAITING, pageable).getContent();
            case REJECTED -> bookingRepository.findByBookerIdAndStatus(userId,
                    BookingStatus.REJECTED, pageable).getContent();
            default -> throw new InvalidStateException("Неизвестное состояние: " + state);
        };

        log.info("Найдено {} бронирований для пользователя с ID={} с состоянием {}",
                bookings.size(), userId, state);

        return mapBookingsToResponseDtos(bookings);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getOwnerBookings(Long userId, BookingState state, int from, int size) {
        log.info("Получение бронирований владельца с ID={}, состояние={}, from={}, size={}",
                userId, state, from, size);

        userService.getById(userId);
        validatePaginationParams(from, size);

        Pageable pageable = PageRequest.of(from / size, size,
                Sort.by("start").descending());
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByOwnerId(userId, pageable).getContent();
            case CURRENT -> bookingRepository.findByOwnerIdAndStartLessThanEqualAndEndGreaterThanEqual(
                    userId, now, now, pageable).getContent();
            case PAST -> bookingRepository.findByOwnerIdAndEndLessThan(userId,
                    now, pageable).getContent();
            case FUTURE -> bookingRepository.findByOwnerIdAndStartGreaterThan(userId,
                    now, pageable).getContent();
            case WAITING -> bookingRepository.findByOwnerIdAndStatus(userId,
                    BookingStatus.WAITING, pageable).getContent();
            case REJECTED -> bookingRepository.findByOwnerIdAndStatus(userId,
                    BookingStatus.REJECTED, pageable).getContent();
            default -> throw new InvalidStateException("Неизвестное состояние: " + state);
        };

        log.info("Найдено {} бронирований для владельца с ID={} с состоянием {}",
                bookings.size(), userId, state);

        return mapBookingsToResponseDtos(bookings);
    }

    private void validateBookingRequest(BookingRequestDto bookingRequestDto) {
        validateBookingDates(bookingRequestDto.getStart(), bookingRequestDto.getEnd());
    }

    private void validateBookingDates(LocalDateTime start, LocalDateTime end) {
        log.debug("Валидация дат бронирования: start={}, end={}", start, end);

        if (start == null || end == null) {
            throw new InvalidDateException("Даты начала и окончания должны быть указаны");
        }

        if (start.isBefore(LocalDateTime.now())) {
            throw new InvalidDateException("Дата начала должна быть в будущем или настоящем");
        }

        if (end.isBefore(start)) {
            throw new InvalidDateException("Дата окончания должна быть позже даты начала");
        }

        if (start.equals(end)) {
            throw new InvalidDateException("Даты начала и окончания не могут совпадать");
        }

        log.debug("Даты бронирования прошли валидацию");
    }

    private void checkForOverlappingBookings(Long itemId, LocalDateTime start, LocalDateTime end) {
        log.debug("Проверка пересечений бронирований для вещи с ID={}, start={}, end={}", itemId, start, end);

        boolean hasOverlap = bookingRepository.existsOverlappingBooking(itemId, start, end);
        if (hasOverlap) {
            log.warn("Обнаружено пересечение бронирований для вещи с ID={}", itemId);
            throw new ItemNotAvailableException("Вещь уже забронирована на указанные даты");
        }

        log.debug("Пересечений бронирований не обнаружено");
    }

    private void validatePaginationParams(int from, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }
    }

    private Item getItemForBooking(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
    }

    private void validateItemForBooking(Item item, Long userId) {
        if (item.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Владелец не может бронировать свою вещь");
        }

        if (!item.getAvailable()) {
            throw new ItemNotAvailableException("Вещь недоступна для бронирования");
        }
    }

    private Booking createBookingEntity(BookingRequestDto bookingRequestDto, Long itemId,
                                        Long userId, Long ownerId) {
        Booking booking = bookingMapper.toEntity(bookingRequestDto);
        booking.setItemId(itemId);
        booking.setBookerId(userId);
        booking.setOwnerId(ownerId);
        booking.setStatus(BookingStatus.WAITING);
        booking.setCreated(LocalDateTime.now());
        return booking;
    }

    private Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));
    }

    private void validateBookingAccess(Booking booking, Long userId) {
        Item item = itemRepository.findById(booking.getItemId()).orElse(null);

        if (!booking.getBookerId().equals(userId) &&
                (item == null || !item.getOwnerId().equals(userId))) {
            throw new AccessDeniedException("Доступ к бронирования запрещен");
        }
    }

    private BookingResponseDto mapToResponseDto(Booking booking) {
        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + booking.getItemId() + " не найдена"));
        User booker = userService.getUserEntityById(booking.getBookerId());
        return bookingMapper.toResponseDto(booking, item, booker);
    }

    private List<BookingResponseDto> mapBookingsToResponseDtos(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Item> itemsMap = loadItemsForBookings(bookings);
        Map<Long, User> usersMap = loadUsersForBookings(bookings);

        log.debug("Загружено {} вещей и {} пользователей для маппинга {} бронирований",
                itemsMap.size(), usersMap.size(), bookings.size());

        return bookings.stream()
                .map(booking -> {
                    Item item = itemsMap.get(booking.getItemId());
                    User booker = usersMap.get(booking.getBookerId());

                    return bookingMapper.toResponseDto(booking, item, booker);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Item> loadItemsForBookings(List<Booking> bookings) {
        List<Long> itemIds = bookings.stream()
                .map(Booking::getItemId)
                .distinct()
                .collect(Collectors.toList());

        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Item> items = itemRepository.findAllById(itemIds);

        if (items.size() != itemIds.size()) {
            List<Long> foundIds = items.stream()
                    .map(Item::getId)
                    .toList();
            List<Long> missingIds = itemIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            if (!missingIds.isEmpty()) {
                log.error("Не найдены вещи с ID: {}", missingIds);
                throw new NotFoundException("Не найдены вещи с ID: " + missingIds);
            }
        }

        return items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
    }

    private Map<Long, User> loadUsersForBookings(List<Booking> bookings) {
        List<Long> userIds = bookings.stream()
                .map(Booking::getBookerId)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userService.getUsersByIds(userIds);

        if (users.size() != userIds.size()) {
            List<Long> foundIds = users.stream()
                    .map(User::getId)
                    .toList();
            List<Long> missingIds = userIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            if (!missingIds.isEmpty()) {
                log.error("Не найдены пользователи с ID: {}", missingIds);
                throw new NotFoundException("Не найдены пользователи с ID: " + missingIds);
            }
        }

        return users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}