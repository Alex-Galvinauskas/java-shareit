package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemService {
    Item create(Long userId, ItemDto itemDto);

    Item update(Long userId, Long itemId, ItemDto itemDto);

    Item getById(Long itemId);

    List<Item> getAllByOwner(Long ownerId, int from, int size);

    List<Item> search(String text, int from, int size);
}