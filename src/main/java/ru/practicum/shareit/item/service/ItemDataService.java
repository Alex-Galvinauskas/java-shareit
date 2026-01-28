package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.dto.BookingInfoDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemDataService {

    private final BookingRepository bookingRepository;
    private final CommentService commentService;

    @Transactional(readOnly = true)
    public void enrichItemWithBookingInfo(ItemDto itemDto, Long ownerId) {
        if (!itemDto.getOwnerId().equals(ownerId)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Pageable firstResult = PageRequest.of(0, 1);

        Page<Booking> lastBookingsPage = bookingRepository
                .findLastBooking(itemDto.getId(), now, firstResult);

        if (!lastBookingsPage.isEmpty()) {
            Booking lastBooking = lastBookingsPage.getContent().getFirst();
            itemDto.setLastBooking(BookingInfoDto.builder()
                    .id(lastBooking.getId())
                    .bookerId(lastBooking.getBookerId())
                    .build());
        }

        Page<Booking> nextBookingsPage = bookingRepository
                .findNextBooking(itemDto.getId(), now, firstResult);

        if (!nextBookingsPage.isEmpty()) {
            Booking nextBooking = nextBookingsPage.getContent().getFirst();
            itemDto.setNextBooking(BookingInfoDto.builder()
                    .id(nextBooking.getId())
                    .bookerId(nextBooking.getBookerId())
                    .build());
        }

        if (lastBookingsPage.isEmpty() && nextBookingsPage.isEmpty()) {
            log.debug("Не найдено активных бронирований для вещи ID={}", itemDto.getId());
        }
    }

    @Transactional(readOnly = true)
    public void enrichItemsWithAdditionalData(List<ItemDto> items, Long ownerId) {
        if (items == null || items.isEmpty()) {
            return;
        }

        log.debug("Обогащение {} элементов дополнительными данными", items.size());

        List<Long> itemIds = items.stream()
                .map(ItemDto::getId)
                .collect(Collectors.toList());

        Map<Long, List<CommentDto>> commentsByItem = commentService.getCommentsForItems(itemIds);
        Map<Long, BookingInfoDto> lastBookingsByItem = loadLastBookingsForItems(itemIds);
        Map<Long, BookingInfoDto> nextBookingsByItem = loadNextBookingsForItems(itemIds);

        items.forEach(itemDto -> {
            itemDto.setComments(commentsByItem.getOrDefault(itemDto.getId(), List.of()));

            if (itemDto.getOwnerId().equals(ownerId)) {
                itemDto.setLastBooking(lastBookingsByItem.get(itemDto.getId()));
                itemDto.setNextBooking(nextBookingsByItem.get(itemDto.getId()));
            }
        });

        log.debug("Обогащение завершено для {} элементов", items.size());
    }

    @Transactional(readOnly = true)
    public void enrichItemWithAdditionalData(ItemDto itemDto, Long userId) {
        log.debug("Обогащение элемента ID={} для пользователя ID={}", itemDto.getId(), userId);

        List<CommentDto> comments = commentService.getCommentsForItem(itemDto.getId());
        itemDto.setComments(comments);

        if (itemDto.getOwnerId().equals(userId)) {
            enrichItemWithBookingInfo(itemDto, userId);
        }
    }

    @Transactional(readOnly = true)
    private Map<Long, BookingInfoDto> loadLastBookingsForItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Загрузка последних бронирований для {} элементов", itemIds.size());

        LocalDateTime now = LocalDateTime.now();
        Map<Long, BookingInfoDto> result = new HashMap<>();

        List<Booking> allLastBookings = bookingRepository
                .findLastBookingsForMultipleItems(itemIds, now, BookingStatus.APPROVED);

        Map<Long, List<Booking>> bookingsByItem = allLastBookings.stream()
                .collect(Collectors.groupingBy(Booking::getItemId));

        bookingsByItem.forEach((itemId, bookings) -> {
            if (!bookings.isEmpty()) {
                Booking lastBooking = bookings.getFirst();
                result.put(itemId, BookingInfoDto.builder()
                        .id(lastBooking.getId())
                        .bookerId(lastBooking.getBookerId())
                        .build());
            }
        });

        log.debug("Загружено последних бронирований для {} элементов", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    private Map<Long, BookingInfoDto> loadNextBookingsForItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Загрузка следующих бронирований для {} элементов", itemIds.size());

        LocalDateTime now = LocalDateTime.now();
        Map<Long, BookingInfoDto> result = new HashMap<>();

        List<Booking> allNextBookings = bookingRepository
                .findNextBookingsForMultipleItems(itemIds, now, BookingStatus.APPROVED);

        Map<Long, List<Booking>> bookingsByItem = allNextBookings.stream()
                .collect(Collectors.groupingBy(Booking::getItemId));

        bookingsByItem.forEach((itemId, bookings) -> {
            if (!bookings.isEmpty()) {
                Booking nextBooking = bookings.getFirst();
                result.put(itemId, BookingInfoDto.builder()
                        .id(nextBooking.getId())
                        .bookerId(nextBooking.getBookerId())
                        .build());
            }
        });

        log.debug("Загружено следующих бронирований для {} элементов", result.size());
        return result;
    }
}