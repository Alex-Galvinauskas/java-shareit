package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemSearchService {

    private static final int MIN_SEARCH_TEXT_LENGTH = 2;

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<Item> searchAvailableItems(String searchText, int from, int size) {
        log.info("Поиск вещей по тексту: '{}', from={}, size={}", searchText, from, size);

        if (isSearchTextInvalid(searchText)) {
            log.info("Текст поиска пустой или слишком короткий, возвращаем пустой список");
            return List.of();
        }

        validatePaginationParams(from, size);

        String trimmedText = searchText.trim();
        Pageable pageable = createPageable(from, size);

        Page<Item> searchPage = itemRepository.searchAvailableItems(trimmedText, pageable);
        List<Item> searchResults = searchPage.getContent();

        log.info("Найдено {} вещей по запросу '{}'", searchResults.size(), trimmedText);
        return searchResults;
    }

    @Transactional(readOnly = true)
    public List<Item> searchByOwner(Long ownerId, int from, int size) {
        log.info("Поиск вещей владельца ID={}, from={}, size={}", ownerId, from, size);

        validatePaginationParams(from, size);
        Pageable pageable = createPageable(from, size);

        Page<Item> ownerItemsPage = itemRepository.findByOwnerId(ownerId, pageable);
        List<Item> items = ownerItemsPage.getContent();

        log.info("Найдено {} вещей для владельца с ID={}", items.size(), ownerId);
        return items;
    }

    public boolean isSearchTextInvalid(String text) {
        return text == null || text.isBlank() || text.trim().length() < MIN_SEARCH_TEXT_LENGTH;
    }

    public void validatePaginationParams(int from, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }
    }

    public Pageable createPageable(int from, int size) {
        int page = from / size;
        return PageRequest.of(page, size, Sort.by("id").ascending());
    }
}