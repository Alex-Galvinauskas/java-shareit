package ru.practicum.shareit.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
public class ItemRequestController {
    private final ItemRequestService itemRequestService;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ResponseEntity<ItemRequestResponseDto> create(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestBody ItemRequestDto itemRequestDto) {
        return ResponseEntity.ok(itemRequestService.create(userId, itemRequestDto));
    }

    @GetMapping
    public ResponseEntity<List<ItemRequestResponseDto>> getOwnRequests(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(itemRequestService.getOwnRequests(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ItemRequestWithItemsDto>> getOtherUsersRequests(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(itemRequestService.getOtherUsersRequests(userId, from, size));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ItemRequestWithItemsDto> getById(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(itemRequestService.getById(userId, requestId));
    }
}