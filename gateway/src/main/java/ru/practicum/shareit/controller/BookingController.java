package ru.practicum.shareit.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.client.BookingClient;
import ru.practicum.shareit.dto.BookingRequestDto;
import ru.practicum.shareit.dto.BookingState;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BookingController {
	private final BookingClient bookingClient;
	private static final String USER_ID_HEADER = "X-Sharer-User-Id";

	@PostMapping
	public ResponseEntity<Object> create(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@Valid @RequestBody BookingRequestDto bookingRequestDto) {
		return bookingClient.create(userId, bookingRequestDto);
	}

	@PatchMapping("/{bookingId}")
	public ResponseEntity<Object> approve(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long bookingId,
			@RequestParam Boolean approved) {
		return bookingClient.approve(userId, bookingId, approved);
	}

	@GetMapping("/{bookingId}")
	public ResponseEntity<Object> getById(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long bookingId) {
		return bookingClient.getById(userId, bookingId);
	}

	@GetMapping
	public ResponseEntity<Object> getUserBookings(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@RequestParam(defaultValue = "ALL") BookingState state,
			@RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
			@RequestParam(defaultValue = "10") @Positive Integer size) {
		return bookingClient.getUserBookings(userId, state, from, size);
	}

	@GetMapping("/owner")
	public ResponseEntity<Object> getOwnerBookings(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@RequestParam(defaultValue = "ALL") BookingState state,
			@RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
			@RequestParam(defaultValue = "10") @Positive Integer size) {
		return bookingClient.getOwnerBookings(userId, state, from, size);
	}
}