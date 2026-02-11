package ru.practicum.shareit.booking.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ResponseEntity<BookingResponseDto> create(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestBody BookingRequestDto bookingRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(userId, bookingRequestDto));
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> approve(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookingId,
            @RequestParam Boolean approved) {
        return ResponseEntity.ok(bookingService.approve(userId, bookingId, approved));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getById(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getById(userId, bookingId));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDto>> getUserBookings(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") BookingState state,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId, state, from, size));
    }

    @GetMapping("/owner")
    public ResponseEntity<List<BookingResponseDto>> getOwnerBookings(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") BookingState state,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return ResponseEntity.ok(bookingService.getOwnerBookings(userId, state, from, size));
    }
}