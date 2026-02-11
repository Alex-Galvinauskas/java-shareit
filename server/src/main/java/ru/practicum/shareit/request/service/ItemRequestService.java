package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestResponseDto create(Long userId, ItemRequestDto itemRequestDto);

    List<ItemRequestResponseDto> getOwnRequests(Long userId);

    List<ItemRequestWithItemsDto> getOtherUsersRequests(Long userId, int from, int size);

    ItemRequestWithItemsDto getById(Long userId, Long requestId);
}