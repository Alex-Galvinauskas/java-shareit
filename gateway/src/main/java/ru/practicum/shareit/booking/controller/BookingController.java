package ru.practicum.shareit.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;


@Controller
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BookingController {
	private final BookingClient bookingClient;

	@GetMapping
	public ResponseEntity<Object> getBookings(@RequestHeader("X-Sharer-User-Id") long userId,
			@RequestParam(name = "state", defaultValue = "all") String stateParam,
			@PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
			@Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
		BookingState state = BookingState.from(stateParam)
				.orElseThrow(() -> new IllegalArgumentException("Unknown state: " + stateParam));
		log.info("Get booking with state {}, userId={}, from={}, size={}", stateParam, userId, from, size);
		return bookingClient.getBookings(userId, state, from, size);
	}

	@PostMapping
	public ResponseEntity<Object> bookItem(@RequestHeader("X-Sharer-User-Id") long userId,
			@RequestBody @Valid BookItemRequestDto requestDto) {
		log.info("Creating booking {}, userId={}", requestDto, userId);
		return bookingClient.bookItem(userId, requestDto);
	}

	@GetMapping("/{bookingId}")
	public ResponseEntity<Object> getBooking(@RequestHeader("X-Sharer-User-Id") long userId,
			@PathVariable Long bookingId) {
		log.info("Get booking {}, userId={}", bookingId, userId);
		return bookingClient.getBooking(userId, bookingId);
	}
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingResponseDto create(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@Valid @RequestBody BookingRequestDto bookingRequestDto) {
		return bookingService.create(userId, bookingRequestDto);
	}

	@PatchMapping("/{bookingId}")
	public BookingResponseDto approve(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long bookingId,
			@RequestParam @NotNull Boolean approved) {
		return bookingService.approve(userId, bookingId, approved);
	}

	@GetMapping("/{bookingId}")
	public BookingResponseDto getById(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long bookingId) {
		return bookingService.getById(userId, bookingId);
	}

	@GetMapping
	public List<BookingResponseDto> getUserBookings(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@RequestParam(defaultValue = "ALL") BookingState state,
			@RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
			@RequestParam(defaultValue = "10") @Positive Integer size) {
		return bookingService.getUserBookings(userId, state, from, size);
	}

	@GetMapping("/owner")
	public List<BookingResponseDto> getOwnerBookings(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@RequestParam(defaultValue = "ALL") BookingState state,
			@RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
			@RequestParam(defaultValue = "10") @Positive Integer size) {
		return bookingService.getOwnerBookings(userId, state, from, size);
	}
}
