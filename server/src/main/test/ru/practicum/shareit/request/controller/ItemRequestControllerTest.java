package ru.practicum.shareit.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты контроллера запросов на вещи")
class ItemRequestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ItemRequestService itemRequestService;

    @InjectMocks
    private ItemRequestController itemRequestController;

    private ObjectMapper objectMapper;
    private ItemRequestDto itemRequestDto;
    private ItemRequestResponseDto itemRequestResponseDto;
    private ItemRequestWithItemsDto itemRequestWithItemsDto;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(itemRequestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        itemRequestDto = ItemRequestDto.builder()
                .description("Нужна дрель для ремонта")
                .build();

        itemRequestResponseDto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Нужна дрель для ремонта")
                .created(LocalDateTime.now())
                .items(List.of())
                .build();

        itemRequestWithItemsDto = ItemRequestWithItemsDto.builder()
                .id(1L)
                .description("Нужна дрель для ремонта")
                .requestorId(1L)
                .created(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    @Test
    @DisplayName("POST /requests - создание запроса")
    void create_ShouldReturnCreatedRequest() throws Exception {
        when(itemRequestService.create(anyLong(), any(ItemRequestDto.class)))
                .thenReturn(itemRequestResponseDto);

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description")
                        .value("Нужна дрель для ремонта"));
    }

    @Test
    @DisplayName("POST /requests - отсутствует заголовок пользователя")
    void create_ShouldReturnBadRequest_WhenMissingUserId() throws Exception {
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Отсутствует обязательный заголовок: X-Sharer-User-Id"));
    }

    @Test
    @DisplayName("POST /requests - некорректный заголовок пользователя")
    void create_ShouldReturnBadRequest_WhenInvalidUserId() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, "не число")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Некорректный формат числа для заголовка: X-Sharer-User-Id"));
    }

    @Test
    @DisplayName("GET /requests - получение собственных запросов")
    void getOwnRequests_ShouldReturnUserRequests() throws Exception {
        when(itemRequestService.getOwnRequests(anyLong()))
                .thenReturn(List.of(itemRequestResponseDto));

        mockMvc.perform(get("/requests")
                        .header(USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].description")
                        .value("Нужна дрель для ремонта"));
    }

    @Test
    @DisplayName("GET /requests - пользователь без запросов")
    void getOwnRequests_ShouldReturnEmptyList_WhenNoRequests() throws Exception {
        when(itemRequestService.getOwnRequests(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(get("/requests")
                        .header(USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /requests/all - получение запросов других пользователей")
    void getOtherUsersRequests_ShouldReturnOtherUsersRequests() throws Exception {
        when(itemRequestService.getOtherUsersRequests(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(itemRequestWithItemsDto));

        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, "1")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].description")
                        .value("Нужна дрель для ремонта"));
    }

    @Test
    @DisplayName("GET /requests/all - значения по умолчанию для пагинации")
    void getOtherUsersRequests_ShouldUseDefaultPagination() throws Exception {
        when(itemRequestService.getOtherUsersRequests(eq(1L),
                eq(0), eq(10)))
                .thenReturn(List.of(itemRequestWithItemsDto));

        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /requests/all - невалидные параметры пагинации")
    void getOtherUsersRequests_ShouldHandleInvalidPagination() throws Exception {
        when(itemRequestService.getOtherUsersRequests(anyLong(),
                anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Размер страницы должен быть больше 0"));

        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, "1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Размер страницы должен быть больше 0"));
    }

    @Test
    @DisplayName("GET /requests/all - отсутствие заголовка пользователя")
    void getOtherUsersRequests_ShouldReturnBadRequest_WhenMissingUserId() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Отсутствует обязательный заголовок: X-Sharer-User-Id"));
    }

    @Test
    @DisplayName("GET /requests/{requestId} - получение запроса по ID")
    void getById_ShouldReturnRequest() throws Exception {
        when(itemRequestService.getById(anyLong(), anyLong()))
                .thenReturn(itemRequestWithItemsDto);

        mockMvc.perform(get("/requests/1")
                        .header(USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description")
                        .value("Нужна дрель для ремонта"))
                .andExpect(jsonPath("$.requestorId").value(1L));
    }

    @Test
    @DisplayName("GET /requests/{requestId} - запрос не найден")
    void getById_ShouldReturnNotFound_WhenRequestNotFound() throws Exception {
        when(itemRequestService.getById(anyLong(), anyLong()))
                .thenThrow(new ru.practicum.shareit.exception.NotFoundException("Запрос не найден"));

        mockMvc.perform(get("/requests/999")
                        .header(USER_ID_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Запрос не найден"));
    }

    @Test
    @DisplayName("GET /requests/{requestId} - отсутствует заголовок пользователя")
    void getById_ShouldReturnBadRequest_WhenMissingUserId() throws Exception {
        mockMvc.perform(get("/requests/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Отсутствует обязательный заголовок: X-Sharer-User-Id"));
    }

    @Test
    @DisplayName("GET /requests/{requestId} - некорректный ID запроса")
    void getById_ShouldReturnBadRequest_WhenInvalidRequestId() throws Exception {
        mockMvc.perform(get("/requests/не_число")
                        .header(USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Некорректный формат числа для идентификатора запроса"));
    }

    @Test
    @DisplayName("GET /requests - отсутствует заголовок пользователя")
    void getOwnRequests_ShouldReturnBadRequest_WhenMissingUserId() throws Exception {
        mockMvc.perform(get("/requests"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Отсутствует обязательный заголовок: X-Sharer-User-Id"));
    }


    @Test
    @DisplayName("GET /requests/all - параметры пагинации не числа")
    void getOtherUsersRequests_ShouldHandleNonNumericParams() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, "1")
                        .param("from", "не число"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Некорректный формат числа для параметра 'from'"));
    }

    @Test
    @DisplayName("POST /requests - невалидный JSON")
    void create_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}