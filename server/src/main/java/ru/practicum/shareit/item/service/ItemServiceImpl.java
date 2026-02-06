package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemMapper itemMapper;
    private final ItemDataService itemDataService;
    private final ItemSearchService itemSearchService;
    private final CommentService commentService;

    @Override
    @Transactional
    public Item create(Long userId, ItemDto itemDto) {
        log.info("Создание новой вещи для пользователя с ID={}", userId);

        userService.getById(userId);

        Item item = itemMapper.toEntity(itemDto, userId);
        item = itemRepository.save(item);

        log.info("Вещь успешно создана с ID={}: name={}, ownerId={}",
                item.getId(), item.getName(), item.getOwnerId());
        return item;
    }

    @Override
    @Transactional
    public Item update(Long userId, Long itemId, ItemDto itemDto) {
        log.info("Обновление вещи с ID={} пользователем с ID={}", itemId, userId);

        userService.getById(userId);

        Item existingItem = itemRepository.findByIdAndOwnerId(itemId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Вещь с id=" + itemId + " не найдена или вы не являетесь владельцем"));

        updateItemFields(existingItem, itemDto);

        Item updatedItem = itemRepository.save(existingItem);
        log.info("Вещь с ID={} успешно обновлена", itemId);

        return updatedItem;
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getById(Long itemId, Long userId) {
        log.debug("Получение вещи по ID={} для пользователя с ID={}", itemId, userId);

        userService.getById(userId);

        Item item = itemRepository.findByIdAccessibleByUser(itemId, userId)
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + itemId + " не найдена или недоступна"));

        ItemDto itemDto = itemMapper.toDto(item);
        itemDataService.enrichItemWithAdditionalData(itemDto, userId);

        log.debug("Вещь с ID={} найдена: name={}, available={}",
                itemId, item.getName(), item.getAvailable());
        return itemDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getAllByOwner(Long ownerId, int from, int size) {
        log.info("Получение всех вещей владельца с ID={}, from={}, size={}", ownerId, from, size);

        userService.getById(ownerId);
        itemSearchService.validatePaginationParams(from, size);

        List<Item> ownerItems = itemSearchService.searchByOwner(ownerId, from, size);
        List<ItemDto> result = ownerItems.stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
        itemDataService.enrichItemsWithAdditionalData(result, ownerId);

        log.info("Найдено {} вещей для владельца с ID={}", result.size(), ownerId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> search(String text, int from, int size) {
        return itemSearchService.searchAvailableItems(text, from, size);
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentCreateDto) {
        return commentService.addComment(userId, itemId, commentCreateDto);
    }

    private void updateItemFields(Item item, ItemDto itemDto) {
        Optional.ofNullable(itemDto.getName())
                .ifPresent(item::setName);

        Optional.ofNullable(itemDto.getDescription())
                .ifPresent(item::setDescription);

        Optional.ofNullable(itemDto.getAvailable())
                .ifPresent(item::setAvailable);
    }
}