package ru.practicum.shareit.booking.controller;

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
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.GlobalExceptionHandler;
import ru.practicum.shareit.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тестирование контроллера бронирований")
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private BookingRequestDto bookingRequestDto;
    private BookingResponseDto bookingResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        bookingRequestDto = BookingRequestDto.builder()
                .itemId(1L)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();

        bookingResponseDto = BookingResponseDto.builder()
                .id(1L)
                .status(BookingStatus.WAITING)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Создание бронирования через контроллер должно возвращать 201")
    void create_shouldReturnCreated() throws Exception {
        when(bookingService.create(anyLong(), any(BookingRequestDto.class)))
                .thenReturn(bookingResponseDto);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("Создание бронирования без заголовка пользователя должно возвращать 400")
    void create_withoutUserIdHeader_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание бронирования с некорректным заголовком пользователя должно возвращать 400")
    void create_withInvalidUserIdHeader_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Подтверждение бронирования через контроллер должно возвращать DTO")
    void approve_shouldReturnOk() throws Exception {
        BookingResponseDto approvedResponse = BookingResponseDto.builder()
                .id(1L)
                .status(BookingStatus.APPROVED)
                .build();

        when(bookingService.approve(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(approvedResponse);

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", "2")
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Подтверждение бронирования без параметра approved должно возвращать 500 (необработанное исключение)")
    void approve_withoutApprovedParam_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Получение бронирования по ID через контроллер должно возвращать DTO")
    void getById_shouldReturnOk() throws Exception {
        when(bookingService.getById(anyLong(), anyLong()))
                .thenReturn(bookingResponseDto);

        mockMvc.perform(get("/bookings/1")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("Получение несуществующего бронирования должно возвращать 404")
    void getById_whenNotFound_shouldReturnNotFound() throws Exception {
        when(bookingService.getById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Бронирование не найдено"));

        mockMvc.perform(get("/bookings/999")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Бронирование не найдено"));
    }

    @Test
    @DisplayName("Получение бронирования без прав доступа должно возвращать 403")
    void getById_whenAccessDenied_shouldReturnForbidden() throws Exception {
        when(bookingService.getById(anyLong(), anyLong()))
                .thenThrow(new AccessDeniedException("Доступ запрещен"));

        mockMvc.perform(get("/bookings/1")
                        .header("X-Sharer-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Доступ запрещен"));
    }

    @Test
    @DisplayName("Получение бронирований пользователя через контроллер должно возвращать список")
    void getUserBookings_shouldReturnList() throws Exception {
        when(bookingService.getUserBookings(anyLong(), any(BookingState.class),
                anyInt(), anyInt()))
                .thenReturn(List.of(bookingResponseDto));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "1")
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    @DisplayName("Получение бронирований пользователя с параметрами по умолчанию должно работать")
    void getUserBookings_withDefaults_shouldReturnList() throws Exception {
        when(bookingService.getUserBookings(anyLong(), eq(BookingState.ALL),
                eq(0), eq(10)))
                .thenReturn(List.of(bookingResponseDto));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("Получение бронирований пользователя с некорректными параметрами пагинации должно возвращать 500 (необработанное исключение валидации)")
    void getUserBookings_withInvalidPagination_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "1")
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Получение бронирований владельца через контроллер должно возвращать список")
    void getOwnerBookings_shouldReturnList() throws Exception {
        when(bookingService.getOwnerBookings(anyLong(), any(BookingState.class),
                anyInt(), anyInt()))
                .thenReturn(List.of(bookingResponseDto));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", "2")
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    @DisplayName("Получение бронирований владельца с некорректным state должно возвращать 400")
    void getOwnerBookings_withInvalidState_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", "2")
                        .param("state", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение бронирований владельца с несуществующим пользователем должно возвращать 404")
    void getOwnerBookings_whenUserNotFound_shouldReturnNotFound() throws Exception {
        when(bookingService.getOwnerBookings(anyLong(), any(BookingState.class),
                anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", "999")
                        .param("state", "ALL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователь не найден"));
    }
}