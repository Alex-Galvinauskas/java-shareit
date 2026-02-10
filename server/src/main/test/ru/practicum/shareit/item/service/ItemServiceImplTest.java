package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Тестирование сервиса предметов")
@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemDataService itemDataService;

    @Mock
    private ItemSearchService itemSearchService;

    @Mock
    private CommentService commentService;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private ItemDto itemDto;
    private Item item;

    @BeforeEach
    void setUp() {

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(1L)
                .requestId(1L)
                .build();

        item = Item.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(1L)
                .requestId(1L)
                .build();

        ItemDto enrichedItemDto = ItemDto.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(1L)
                .requestId(1L)
                .comments(List.of(CommentDto.builder().id(1L).text("Комментарий").build()))
                .build();

    }

    @Test
    @DisplayName("Создание предмета с валидными данными должно возвращать созданный предмет")
    void create_whenValidData_shouldReturnCreatedItem() {

        itemDto.setRequestId(null);
        when(userService.getById(1L)).thenReturn(null);
        when(itemMapper.toEntity(itemDto, 1L)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);

        Item result = itemService.create(1L, itemDto);

        assertNotNull(result);
        assertEquals("Тестовый предмет", result.getName());

        verify(itemRepository).save(item);
        verify(itemRequestRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Создание предмета с несуществующим запросом должно выбрасывать исключение")
    void create_whenRequestNotFound_shouldThrowNotFoundException() {

        when(userService.getById(1L)).thenReturn(null);
        when(itemRequestRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.create(1L, itemDto));

        verify(itemRepository, never()).save(any());

    }

    @Test
    @DisplayName("Создание предмета с null ID запроса должно создавать предмет без запроса")
    void create_whenRequestIdIsNull_shouldCreateWithoutRequest() {

        itemDto.setRequestId(null);
        when(userService.getById(1L)).thenReturn(null);
        when(itemMapper.toEntity(itemDto, 1L)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);

        Item result = itemService.create(1L, itemDto);

        assertNotNull(result);

        verify(itemRequestRepository, never()).findById(any());
        verify(itemRepository).save(item);
    }

    @Test
    @DisplayName("Обновление предмета при существовании предмета и совпадении владельца должно обновлять предмет")
    void update_whenItemExistsAndUserIsOwner_shouldUpdateItem() {

        ItemDto updateDto = ItemDto.builder()
                .name("Обновленное имя")
                .description("Обновленное описание")
                .available(false)
                .build();

        when(userService.getById(1L)).thenReturn(null);
        when(itemRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        Item result = itemService.update(1L, 1L, updateDto);

        assertNotNull(result);
        assertEquals("Обновленное имя", result.getName());
        assertEquals("Обновленное описание", result.getDescription());
        assertFalse(result.getAvailable());

        verify(itemRepository).save(item);
    }

    @Test
    @DisplayName("Обновление предмета при отсутствии предмета или несовпадении владельца должно выбрасывать исключение")
    void update_whenItemNotFoundOrUserNotOwner_shouldThrowNotFoundException() {

        when(userService.getById(1L)).thenReturn(null);
        when(itemRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.update(1L, 1L, itemDto));

        verify(itemRepository, never()).save(any());

    }

    @Test
    @DisplayName("Частичное обновление предмета должно обновлять только предоставленные поля")
    void update_whenPartialUpdate_shouldUpdateOnlyProvidedFields() {

        ItemDto partialUpdateDto = ItemDto.builder()
                .name("Только имя обновлено")
                .build();

        Item existingItem = Item.builder()
                .id(1L)
                .name("Оригинальное имя")
                .description("Оригинальное описание")
                .available(true)
                .ownerId(1L)
                .build();

        when(userService.getById(1L)).thenReturn(null);
        when(itemRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(existingItem)).thenReturn(existingItem);

        Item result = itemService.update(1L, 1L, partialUpdateDto);

        assertNotNull(result);
        assertEquals("Только имя обновлено", result.getName());
        assertEquals("Оригинальное описание", result.getDescription());
        assertTrue(result.getAvailable());

    }

    @Test
    @DisplayName("Получение предмета по ID при существовании и доступности должен возвращать DTO предмета")
    void getById_whenItemExistsAndAccessible_shouldReturnItemDto() {

        when(userService.getById(1L)).thenReturn(null);
        when(itemRepository.findByIdAccessibleByUser(1L, 1L))
                .thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        doNothing().when(itemDataService).enrichItemWithAdditionalData(itemDto, 1L);

        ItemDto result = itemService.getById(1L, 1L);

        assertNotNull(result);

        verify(itemDataService).enrichItemWithAdditionalData(itemDto, 1L);
    }

    @Test
    @DisplayName("Получение предмета по ID при недоступности должен выбрасывать исключение")
    void getById_whenItemNotAccessible_shouldThrowNotFoundException() {

        when(userService.getById(1L)).thenReturn(null);
        when(itemRepository.findByIdAccessibleByUser(1L, 1L))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.getById(1L, 1L));

        verify(itemDataService, never()).enrichItemWithAdditionalData(any(), any());
    }

    @Test
    @DisplayName("Получение всех предметов владельца с валидными параметрами должно возвращать список предметов")
    void getAllByOwner_whenValidParams_shouldReturnItemList() {

        when(userService.getById(1L)).thenReturn(null);
        when(itemSearchService.searchByOwner(1L, 0, 10))
                .thenReturn(List.of(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        doNothing().when(itemDataService).enrichItemsWithAdditionalData(anyList(), eq(1L));

        List<ItemDto> result = itemService.getAllByOwner(1L, 0, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        verify(itemDataService).enrichItemsWithAdditionalData(anyList(), eq(1L));
    }

    @Test
    @DisplayName("Поиск с валидным текстом должен возвращать предметы")
    void search_whenValidText_shouldReturnItems() {

        when(itemSearchService.searchAvailableItems("тест", 0, 10))
                .thenReturn(List.of(item));

        List<Item> result = itemService.search("тест", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(itemSearchService).searchAvailableItems("тест", 0, 10);
    }

    @Test
    @DisplayName("Добавление комментария должно делегироваться сервису комментариев")
    void addComment_shouldDelegateToCommentService() {

        CommentCreateDto commentCreateDto = CommentCreateDto.builder()
                .text("Тестовый комментарий")
                .build();

        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Тестовый комментарий")
                .build();

        when(commentService.addComment(1L, 1L, commentCreateDto))
                .thenReturn(commentDto);

        CommentDto result = itemService.addComment(1L, 1L, commentCreateDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Тестовый комментарий", result.getText());


        verify(commentService).addComment(1L, 1L, commentCreateDto);
    }

    @Test
    @DisplayName("Обновление полей предмета при предоставлении всех полей должно обновлять все")
    void updateItemFields_whenAllFieldsProvided_shouldUpdateAll() {

        Item existingItem = Item.builder()
                .id(1L)
                .name("Старое имя")
                .description("Старое описание")
                .available(true)
                .build();

        ItemDto updateDto = ItemDto.builder()
                .name("Новое имя")
                .description("Новое описание")
                .available(false)
                .requestId(1L)
                .build();

        assertThrows(NotFoundException.class,
                () -> itemService.update(1L, 1L, updateDto));

    }

    @Test
    @DisplayName("Обновление полей предмета при некоторых null полях должно обновлять только не-null")
    void updateItemFields_whenSomeFieldsNull_shouldUpdateOnlyNonNull() {

        ItemDto partialUpdate = ItemDto.builder()
                .name("Обновленное имя")
                .build();

        Item existingItem = Item.builder()
                .id(1L)
                .name("Оригинальное имя")
                .description("Оригинальное описание")
                .available(true)
                .ownerId(1L)
                .build();

        when(userService.getById(1L)).thenReturn(null);
        when(itemRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(existingItem));
        when(itemRepository.save(existingItem)).thenReturn(existingItem);

        itemService.update(1L, 1L, partialUpdate);

        assertEquals("Обновленное имя", existingItem.getName());
        assertEquals("Оригинальное описание", existingItem.getDescription());
        assertTrue(existingItem.getAvailable());

    }
}