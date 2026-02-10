package ru.practicum.shareit.user.controller;

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
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты контроллера пользователей")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Создание пользователя - успешно")
    void createUser_shouldReturnCreatedUser() throws Exception {
        UserDto userDto = UserDto.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        UserDto createdUser = UserDto.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        when(userService.create(any(UserDto.class))).thenReturn(createdUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).create(any(UserDto.class));
    }

    @Test
    @DisplayName("Обновление пользователя - успешно")
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        Long userId = 1L;
        UserDto updateDto = UserDto.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        UserDto updatedUser = UserDto.builder()
                .id(userId)
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        when(userService.update(eq(userId), any(UserDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(patch("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService, times(1)).update(eq(userId),
                any(UserDto.class));
    }

    @Test
    @DisplayName("Получение пользователя по ID - успешно")
    void getUserById_shouldReturnUser() throws Exception {
        Long userId = 1L;
        UserDto userDto = UserDto.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .build();

        when(userService.getById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService, times(1)).getById(userId);
    }

    @Test
    @DisplayName("Получение всех пользователей с пагинацией - успешно")
    void getAllUsers_shouldReturnPaginatedUsers() throws Exception {
        UserDto user1 = UserDto.builder().id(1L).name("User1").email("user1@example.com").build();
        UserDto user2 = UserDto.builder().id(2L).name("User2").email("user2@example.com").build();

        when(userService.getAll(0, 10)).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(userService, times(1)).getAll(0, 10);
    }

    @Test
    @DisplayName("Получение всех пользователей с дефолтными параметрами - успешно")
    void getAllUsers_withDefaultParams_shouldReturnUsers() throws Exception {
        UserDto user = UserDto.builder().id(1L).name("User").email("user@example.com").build();

        when(userService.getAll(0, 10)).thenReturn(List.of(user));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(userService, times(1)).getAll(0, 10);
    }

    @Test
    @DisplayName("Удаление пользователя - успешно")
    void deleteUser_shouldReturnNoContent() throws Exception {
        Long userId = 1L;

        doNothing().when(userService).delete(userId);

        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isOk());

        verify(userService, times(1)).delete(userId);
    }
}