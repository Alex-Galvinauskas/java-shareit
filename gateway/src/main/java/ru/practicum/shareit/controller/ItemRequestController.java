package ru.practicum.shareit.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.client.ItemRequestClient;
import ru.practicum.shareit.dto.ItemRequestDto;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestClient itemRequestClient;

    @PostMapping
    public ResponseEntity<String> create(
            @RequestHeader("X-Sharer-User-Id") long userId,
            @Valid @RequestBody ItemRequestDto itemRequestDto) {
        return itemRequestClient.create(userId, itemRequestDto);
    }

    @GetMapping
    public ResponseEntity<String> getOwnRequests(
            @RequestHeader("X-Sharer-User-Id") long userId) {
        return itemRequestClient.getOwnRequests(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<String> getOtherUsersRequests(
            @RequestHeader("X-Sharer-User-Id") long userId,
            @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
            @Positive @RequestParam(defaultValue = "10") Integer size) {
        return itemRequestClient.getOtherUsersRequests(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<String> getById(
            @RequestHeader("X-Sharer-User-Id") long userId,
            @PathVariable Long requestId) {
        return itemRequestClient.getById(userId, requestId);
    }
}