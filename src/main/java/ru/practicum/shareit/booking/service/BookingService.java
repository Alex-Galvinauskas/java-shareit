package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.util.List;

public interface BookingService {
    BookingResponseDto create(Long userId, BookingRequestDto bookingRequestDto);

    BookingResponseDto approve(Long userId, Long bookingId, Boolean approved);

    BookingResponseDto getById(Long userId, Long bookingId);

    List<BookingResponseDto> getUserBookings(Long userId, BookingState state, int from, int size);

    List<BookingResponseDto> getOwnerBookings(Long userId, BookingState state, int from, int size);
}