package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.exception.GlobalExceptionHandler;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование контроллера предметов")
class ItemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ItemService itemService;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemController itemController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ItemDto itemDto;
    private Item item;
    private CommentDto commentDto;
    private CommentCreateDto commentCreateDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(itemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(1L)
                .build();

        item = Item.builder()
                .id(1L)
                .name("Тестовый предмет")
                .description("Тестовое описание")
                .available(true)
                .ownerId(1L)
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("Тестовый комментарий")
                .authorName("Тестовый пользователь")
                .created(LocalDateTime.now())
                .build();

        commentCreateDto = CommentCreateDto.builder()
                .text("Тестовый комментарий")
                .build();
    }

    @Test
    @DisplayName("Создание предмета через контроллер должно возвращать 200")
    void create_shouldReturnOk() throws Exception {
        when(itemService.create(anyLong(), any(ItemDto.class))).thenReturn(item);
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Тестовый предмет"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("Создание предмета без заголовка пользователя должно возвращать 500")
    void create_withoutUserIdHeader_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание предмета с некорректным заголовком пользователя должно возвращать 500")
    void create_withInvalidUserIdHeader_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Обновление предмета через контроллер должно возвращать 200")
    void update_shouldReturnOk() throws Exception {
        ItemDto updatedDto = ItemDto.builder()
                .name("Обновленное имя")
                .description("Обновленное описание")
                .build();

        Item updatedItem = Item.builder()
                .id(1L)
                .name("Обновленное имя")
                .description("Обновленное описание")
                .available(true)
                .ownerId(1L)
                .build();

        ItemDto resultDto = ItemDto.builder()
                .id(1L)
                .name("Обновленное имя")
                .description("Обновленное описание")
                .available(true)
                .ownerId(1L)
                .build();

        when(itemService.update(anyLong(), anyLong(),
                any(ItemDto.class))).thenReturn(updatedItem);
        when(itemMapper.toDto(updatedItem)).thenReturn(resultDto);

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Обновленное имя"))
                .andExpect(jsonPath("$.description").value("Обновленное описание"));
    }

    @Test
    @DisplayName("Обновление несуществующего предмета должно возвращать 404")
    void update_whenItemNotFound_shouldReturnNotFound() throws Exception {
        when(itemService.update(anyLong(), anyLong(), any(ItemDto.class)))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(patch("/items/999")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Вещь не найдена"));
    }

    @Test
    @DisplayName("Получение предмета по ID через контроллер должно возвращать 200")
    void getById_shouldReturnOk() throws Exception {
        when(itemService.getById(anyLong(), anyLong())).thenReturn(itemDto);

        mockMvc.perform(get("/items/1")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Тестовый предмет"));
    }

    @Test
    @DisplayName("Получение несуществующего предмета должно возвращать 404")
    void getById_whenItemNotFound_shouldReturnNotFound() throws Exception {
        when(itemService.getById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(get("/items/999")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Вещь не найдена"));
    }

    @Test
    @DisplayName("Получение всех предметов владельца должно возвращать список")
    void getAllByOwner_shouldReturnList() throws Exception {
        when(itemService.getAllByOwner(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(itemDto));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Тестовый предмет"));
    }

    @Test
    @DisplayName("Получение всех предметов владельца с параметрами по умолчанию должно работать")
    void getAllByOwner_withDefaults_shouldReturnList() throws Exception {
        when(itemService.getAllByOwner(anyLong(), eq(0), eq(10)))
                .thenReturn(List.of(itemDto));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение всех предметов владельца с некорректными параметрами пагинации должно возвращать 500")
    void getAllByOwner_withInvalidPagination_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1")
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Поиск предметов через контроллер должно возвращать список")
    void search_shouldReturnList() throws Exception {
        when(itemService.search(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        mockMvc.perform(get("/items/search")
                        .param("text", "тест")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Тестовый предмет"));
    }

    @Test
    @DisplayName("Поиск предметов с пустым текстом должно возвращать пустой список")
    void search_withEmptyText_shouldReturnEmptyList() throws Exception {
        when(itemService.search(eq(""), anyInt(),
                anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/items/search")
                        .param("text", "")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Поиск предметов с параметрами по умолчанию должно работать")
    void search_withDefaults_shouldReturnList() throws Exception {
        when(itemService.search(anyString(), eq(0), eq(10)))
                .thenReturn(List.of(item));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        mockMvc.perform(get("/items/search")
                        .param("text", "тест"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Добавление комментария через контроллер должно возвращать 200")
    void addComment_shouldReturnOk() throws Exception {
        when(itemService.addComment(anyLong(), anyLong(),
                any(CommentCreateDto.class)))
                .thenReturn(commentDto);

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentCreateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Тестовый комментарий"))
                .andExpect(jsonPath("$.authorName").value("Тестовый пользователь"));
    }

    @Test
    @DisplayName("Добавление комментария без заголовка пользователя должно возвращать 500")
    void addComment_withoutUserIdHeader_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(post("/items/1/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentCreateDto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("Добавление комментария для несуществующего предмета должно возвращать 404")
    void addComment_whenItemNotFound_shouldReturnNotFound() throws Exception {
        when(itemService.addComment(anyLong(), anyLong(),
                any(CommentCreateDto.class)))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(post("/items/999/comment")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentCreateDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Вещь не найдена"));
    }
}