package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public Item create(Long userId, ItemDto itemDto) {
        log.info("Создание новой вещи для пользователя с ID={}, данные: {}", userId, itemDto);

        userService.getById(userId);

        Item item = itemMapper.toEntity(itemDto, userId);
        item = itemRepository.save(item);

        log.info("Вещь успешно создана с ID={}: {}", item.getId(), item);
        return item;
    }

    @Override
    @Transactional
    public Item update(Long userId, Long itemId, ItemDto itemDto) {
        log.info("Обновление вещи с ID={} пользователем с ID={}, новые данные: {}", itemId, userId, itemDto);

        userService.getById(userId);

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

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
            existingItem = itemRepository.save(existingItem);
            log.info("Вещь с ID={} успешно обновлена: {}", itemId, existingItem);
        } else {
            log.info("Вещь с ID={} не была изменена, все поля null", itemId);
        }

        return existingItem;
    }

    @Override
    @Transactional(readOnly = true)
    public Item getById(Long itemId) {
        log.debug("Получение вещи по ID={}", itemId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        log.debug("Вещь с ID={} найдена: {}", itemId, item);
        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> getAllByOwner(Long ownerId, int from, int size) {
        log.info("Получение всех вещей владельца с ID={}, from={}, size={}", ownerId, from, size);

        userService.getById(ownerId);

        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Item> ownerItems = itemRepository.findByOwnerId(ownerId, pageable);

        log.info("Найдено {} вещей для владельца с ID={}", ownerItems.size(), ownerId);
        return ownerItems;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> search(String text, int from, int size) {
        log.info("Поиск вещей по тексту: '{}', from={}, size={}", text, from, size);

        if (text == null || text.isBlank()) {
            log.info("Текст поиска пустой, возвращаем пустой список");
            return List.of();
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Item> searchResults = itemRepository.searchAvailableItems(text, pageable);

        log.info("Найдено {} вещей по запросу '{}'", searchResults.size(), text);
        return searchResults;
    }
}