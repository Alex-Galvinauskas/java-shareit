package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.service.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final Map<Long, Item> items = new HashMap<>();
    private Long currentId = 1L;
    private final UserService userService;
    private final ItemMapper itemMapper;

    @Override
    public Item create(Long userId, ItemDto itemDto) {
        log.info("Создание новой вещи для пользователя с ID={}, данные: {}", userId, itemDto);

        userService.getById(userId);

        Item item = itemMapper.toEntity(itemDto, userId);
        item.setId(currentId++);
        items.put(item.getId(), item);

        log.info("Вещь успешно создана с ID={}: {}", item.getId(), item);
        return item;
    }

    @Override
    public Item update(Long userId, Long itemId, ItemDto itemDto) {
        log.info("Обновление вещи с ID={} пользователем с ID={}, новые данные: {}", itemId, userId, itemDto);

        userService.getById(userId);

        Item existingItem = items.get(itemId);
        if (existingItem == null) {
            log.warn("Вещь с ID={} не найдена при попытке обновления пользователем с ID={}", itemId, userId);
            throw new NotFoundException("Вещь с id=" + itemId + " не найдена");
        }

        if (!existingItem.getOwnerId().equals(userId)) {
            log.warn("Пользователь с ID={} не является владельцем вещи с ID={}", userId, itemId);
            throw new NotFoundException("Пользователь не является владельцем вещи");
        }

        boolean changed = false;
        if (itemDto.getName() != null) {
            log.debug("Обновление названия вещи с ID={}: '{}' -> '{}'", itemId, existingItem.getName(), itemDto.getName());
            existingItem.setName(itemDto.getName());
            changed = true;
        }
        if (itemDto.getDescription() != null) {
            log.debug("Обновление описания вещи с ID={}", itemId);
            existingItem.setDescription(itemDto.getDescription());
            changed = true;
        }
        if (itemDto.getAvailable() != null) {
            log.debug("Обновление доступности вещи с ID={}: {} -> {}", itemId, existingItem.getAvailable(), itemDto.getAvailable());
            existingItem.setAvailable(itemDto.getAvailable());
            changed = true;
        }

        if (changed) {
            items.put(itemId, existingItem);
            log.info("Вещь с ID={} успешно обновлена: {}", itemId, existingItem);
        } else {
            log.info("Вещь с ID={} не была изменена, все поля null", itemId);
        }

        return existingItem;
    }

    @Override
    public Item getById(Long itemId) {
        log.debug("Получение вещи по ID={}", itemId);

        Item item = items.get(itemId);
        if (item == null) {
            log.warn("Вещь с ID={} не найдена", itemId);
            throw new NotFoundException("Вещь с id=" + itemId + " не найдена");
        }

        log.debug("Вещь с ID={} найдена: {}", itemId, item);
        return item;
    }

    @Override
    public List<Item> getAllByOwner(Long ownerId) {
        log.info("Получение всех вещей владельца с ID={}", ownerId);

        userService.getById(ownerId);

        List<Item> ownerItems = items.values().stream()
                .filter(item -> item.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());

        log.info("Найдено {} вещей для владельца с ID={}", ownerItems.size(), ownerId);
        return ownerItems;
    }

    @Override
    public List<Item> search(String text) {
        log.info("Поиск вещей по тексту: '{}'", text);

        if (text == null || text.isBlank()) {
            log.info("Текст поиска пустой, возвращаем пустой список");
            return new ArrayList<>();
        }

        String searchText = text.toLowerCase();
        List<Item> searchResults = items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item ->
                        (item.getName() != null && item.getName().toLowerCase().contains(searchText)) ||
                                (item.getDescription() != null
                                        && item.getDescription().toLowerCase().contains(searchText))
                )
                .collect(Collectors.toList());

        log.info("Найдено {} вещей по запросу '{}'", searchResults.size(), text);
        return searchResults;
    }
}