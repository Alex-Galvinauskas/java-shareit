package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Тестирование сервиса поиска предметов")
@ExtendWith(MockitoExtension.class)
class ItemSearchServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemSearchService itemSearchService;

    private Item item;

    @BeforeEach
    void setUp() {

        item = Item.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(1L)
                .build();

    }

    @Test
    @DisplayName("Поиск доступных предметов с валидным текстом должен возвращать предметы")
    void searchAvailableItems_whenValidSearchText_shouldReturnItems() {

        String searchText = "тест";
        Page<Item> page = new PageImpl<>(List.of(item));

        when(itemRepository.searchAvailableItems(eq(searchText),
                any(Pageable.class)))
                .thenReturn(page);

        List<Item> result = itemSearchService.searchAvailableItems(searchText, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Тестовый предмет", result.getFirst().getName());


        verify(itemRepository).searchAvailableItems(eq(searchText),
                any(Pageable.class));
    }

    @Test
    @DisplayName("Поиск доступных предметов с пустым текстом должен возвращать пустой список")
    void searchAvailableItems_whenSearchTextIsBlank_shouldReturnEmptyList() {

        List<Item> result = itemSearchService.searchAvailableItems(" ", 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(itemRepository, never()).searchAvailableItems(anyString(), any());
    }

    @Test
    @DisplayName("Поиск доступных предметов с null текстом должен возвращать пустой список")
    void searchAvailableItems_whenSearchTextIsNull_shouldReturnEmptyList() {

        List<Item> result = itemSearchService.searchAvailableItems(null, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(itemRepository, never()).searchAvailableItems(anyString(), any());
    }

    @Test
    @DisplayName("Поиск доступных предметов со слишком коротким текстом должен возвращать пустой список")
    void searchAvailableItems_whenSearchTextTooShort_shouldReturnEmptyList() {

        List<Item> result = itemSearchService.searchAvailableItems("а", 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(itemRepository, never()).searchAvailableItems(anyString(), any());
    }

    @Test
    @DisplayName("Поиск доступных предметов с невалидными параметрами пагинации должен выбрасывать исключение")
    void searchAvailableItems_whenInvalidPaginationParams_shouldThrowException() {

        verify(itemRepository, never()).searchAvailableItems(anyString(), any());

    }

    @Test
    @DisplayName("Поиск по владельцу с валидными параметрами должен возвращать предметы")
    void searchByOwner_whenValidParams_shouldReturnItems() {

        Page<Item> page = new PageImpl<>(List.of(item));
        when(itemRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        List<Item> result = itemSearchService.searchByOwner(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getOwnerId());


        verify(itemRepository).findByOwnerId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Поиск по владельцу при отсутствии предметов должен возвращать пустой список")
    void searchByOwner_whenNoItemsFound_shouldReturnEmptyList() {

        Page<Item> emptyPage = new PageImpl<>(Collections.emptyList());
        when(itemRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        List<Item> result = itemSearchService.searchByOwner(1L, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    @DisplayName("Поиск по владельцу с невалидными параметрами пагинации должен выбрасывать исключение")
    void searchByOwner_whenInvalidPaginationParams_shouldThrowException() {

        assertThrows(IllegalArgumentException.class,
                () -> itemSearchService.searchByOwner(1L, -1, 10));

        assertThrows(IllegalArgumentException.class,
                () -> itemSearchService.searchByOwner(1L, 0, 0));

    }

    @Test
    @DisplayName("Проверка невалидности текста поиска для различных входных данных должна возвращать корректные результаты")
    void isSearchTextInvalid_whenVariousInputs_shouldReturnCorrectResults() {

        assertTrue(itemSearchService.isSearchTextInvalid(null));

        assertTrue(itemSearchService.isSearchTextInvalid(""));

        assertTrue(itemSearchService.isSearchTextInvalid(" "));

        assertTrue(itemSearchService.isSearchTextInvalid("а"));

        assertFalse(itemSearchService.isSearchTextInvalid("аб"));

        assertFalse(itemSearchService.isSearchTextInvalid("тест"));

        assertFalse(itemSearchService.isSearchTextInvalid("  тест  "));

    }

    @Test
    @DisplayName("Валидация параметров пагинации с валидными параметрами не должна выбрасывать исключение")
    void validatePaginationParams_whenValidParams_shouldNotThrow() {

        assertDoesNotThrow(() -> itemSearchService.validatePaginationParams(0, 10));

        assertDoesNotThrow(() -> itemSearchService.validatePaginationParams(100, 50));

    }


    @Test
    @DisplayName("Создание Pageable должно создавать корректный Pageable")
    void createPageable_shouldCreateCorrectPageable() {

        Pageable pageable1 = itemSearchService.createPageable(0, 10);
        assertEquals(0, pageable1.getPageNumber());

        Pageable pageable2 = itemSearchService.createPageable(10, 10);
        assertEquals(1, pageable2.getPageNumber());

        Pageable pageable3 = itemSearchService.createPageable(25, 5);
        assertEquals(5, pageable3.getPageNumber());

        assertTrue(pageable1.getSort().isSorted());
    }

    @Test
    @DisplayName("Поиск доступных предметов с граничными значениями пагинации должен работать корректно")
    void searchAvailableItems_whenBoundaryPaginationParams_shouldWorkCorrectly() {

        String searchText = "тест";
        Page<Item> page = new PageImpl<>(Collections.emptyList());

        when(itemRepository.searchAvailableItems(eq(searchText),
                any(Pageable.class)))
                .thenReturn(page);

        assertDoesNotThrow(() -> itemSearchService.searchAvailableItems(searchText, 0, 1));
        assertDoesNotThrow(() -> itemSearchService.searchAvailableItems(searchText,
                Integer.MAX_VALUE - 10, 10));

    }

    @Test
    @DisplayName("Поиск доступных предметов с большой страницей должен создавать правильный Pageable")
    void searchAvailableItems_whenLargePageSize_shouldCreateCorrectPageable() {

        String searchText = "тест";
        Page<Item> page = new PageImpl<>(Collections.emptyList());

        when(itemRepository.searchAvailableItems(eq(searchText),
                any(Pageable.class)))
                .thenReturn(page);

        List<Item> result = itemSearchService.searchAvailableItems(searchText, 100, 50);

        assertNotNull(result);
        verify(itemRepository).searchAvailableItems(eq(searchText),
                argThat(pageable ->
                pageable.getPageNumber() == 2 && pageable.getPageSize() == 50));

    }

    @Test
    @DisplayName("Поиск по владельцу с большим количеством предметов должен возвращать пагинированный результат")
    void searchByOwner_whenManyItems_shouldReturnPaginatedResult() {

        List<Item> items = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            items.add(Item.builder()
                    .id(i)
                    .name("Предмет " + i)
                    .ownerId(1L)
                    .available(true)
                    .build());
        }

        Page<Item> page = new PageImpl<>(items.subList(10, 20),
                PageRequest.of(1, 10, Sort.by("id").ascending()),
                items.size());

        when(itemRepository.findByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        List<Item> result = itemSearchService.searchByOwner(1L, 10, 10);

        assertNotNull(result);
        assertEquals(10, result.size());
        assertEquals("Предмет 11", result.getFirst().getName());

    }

    @Test
    @DisplayName("Создание Pageable с максимальными значениями должно работать корректно")
    void createPageable_whenMaxValues_shouldWorkCorrectly() {

        Pageable pageable1 = itemSearchService.createPageable(Integer.MAX_VALUE - 100, 100);
        assertEquals((Integer.MAX_VALUE - 100) / 100, pageable1.getPageNumber());

        Pageable pageable2 = itemSearchService.createPageable(0, Integer.MAX_VALUE);
        assertEquals(0, pageable2.getPageNumber());
        assertEquals(Integer.MAX_VALUE, pageable2.getPageSize());

    }

    @Test
    @DisplayName("Поиск с текстом содержащим специальные символы должен обрабатываться корректно")
    void searchAvailableItems_whenTextWithSpecialCharacters_shouldHandleCorrectly() {

        String searchText = "тест%_'\"";
        Page<Item> page = new PageImpl<>(List.of(item));

        when(itemRepository.searchAvailableItems(eq(searchText.trim()),
                any(Pageable.class)))
                .thenReturn(page);

        List<Item> result = itemSearchService.searchAvailableItems(searchText, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository).searchAvailableItems(eq(searchText.trim()),
                any(Pageable.class));

    }

    @Test
    @DisplayName("Проверка валидности текста поиска с Unicode символами")
    void isSearchTextInvalid_whenUnicodeCharacters_shouldReturnCorrectResults() {

        assertTrue(itemSearchService.isSearchTextInvalid("a"));
        assertFalse(itemSearchService.isSearchTextInvalid("ab"));
        assertFalse(itemSearchService.isSearchTextInvalid("теst"));

        assertTrue(itemSearchService.isSearchTextInvalid("s"));
        assertFalse(itemSearchService.isSearchTextInvalid("as"));


        String singleCharEmoji = "😀";

        assertFalse(itemSearchService.isSearchTextInvalid("😀"));

        assertFalse(itemSearchService.isSearchTextInvalid("😀😀"));

        assertTrue(itemSearchService.isSearchTextInvalid(" s "));
        assertFalse(itemSearchService.isSearchTextInvalid(" as "));

    }

    @Test
    @DisplayName("Поиск по несуществующему владельцу должен возвращать пустой список")
    void searchByOwner_whenOwnerDoesNotExist_shouldReturnEmptyList() {

        Page<Item> emptyPage = new PageImpl<>(Collections.emptyList());
        when(itemRepository.findByOwnerId(eq(999L), any(Pageable.class)))
                .thenReturn(emptyPage);

        List<Item> result = itemSearchService.searchByOwner(999L, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}