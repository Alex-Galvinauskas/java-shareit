package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemService {
    Item create(Long userId, ItemDto itemDto);

    Item update(Long userId, Long itemId, ItemDto itemDto);

    ItemDto getById(Long itemId, Long userId);

    List<ItemDto> getAllByOwner(Long ownerId, int from, int size);

    List<Item> search(String text, int from, int size);

    CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentCreateDto);
}