package ru.practicum.shareit.item.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final ItemMapper itemMapper;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ResponseEntity<ItemDto> create(@RequestHeader(USER_ID_HEADER) Long userId,
                                          @RequestBody ItemDto itemDto) {
        return ResponseEntity.ok(itemMapper.toDto(itemService.create(userId, itemDto)));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemDto> update(@RequestHeader(USER_ID_HEADER) Long userId,
                                          @PathVariable Long itemId,
                                          @RequestBody ItemDto itemDto) {
        return ResponseEntity.ok(itemMapper.toDto(itemService.update(userId, itemId, itemDto)));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getById(@RequestHeader(USER_ID_HEADER) Long userId,
                                           @PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.getById(itemId, userId));
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> getAllByOwner(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return ResponseEntity.ok(itemService.getAllByOwner(userId, from, size));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> search(
            @RequestParam String text,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        List<ItemDto> result = itemService.search(text, from, size).stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<CommentDto> addComment(@RequestHeader(USER_ID_HEADER) Long userId,
                                                 @PathVariable Long itemId,
                                                 @Valid @RequestBody CommentCreateDto commentCreateDto) {
        return ResponseEntity.ok(itemService.addComment(userId, itemId, commentCreateDto));
    }
}