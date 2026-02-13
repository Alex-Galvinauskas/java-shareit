package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты пользователей")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private UserDto testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = UserDto.builder()
                .name("Integration Test User")
                .email("integration@test.com")
                .build();
    }

    @Test
    @DisplayName("Полный жизненный цикл пользователя: создание -> получение -> обновление -> удаление")
    void fullUserLifecycle_shouldWorkCorrectly() throws Exception {
        String createResponse = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Integration Test User"))
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser = objectMapper.readValue(createResponse, UserDto.class);
        Long userId = createdUser.getId();
        assertNotNull(userId);

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Integration Test User"))
                .andExpect(jsonPath("$.email").value("integration@test.com"));

        UserDto updateDto = UserDto.builder()
                .name("Updated Integration User")
                .email("updated@integration.com")
                .build();

        mockMvc.perform(patch("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Updated Integration User"))
                .andExpect(jsonPath("$.email").value("updated@integration.com"));

        mockMvc.perform(get("/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId));

        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание пользователя с существующим email - должно вернуть ошибку")
    void createUser_withDuplicateEmail_shouldReturnError() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk());

        UserDto duplicateUser = UserDto.builder()
                .name("Another User")
                .email("integration@test.com")
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Обновление пользователя с email другого пользователя - должно вернуть ошибку")
    void updateUser_withAnotherUserEmail_shouldReturnError() throws Exception {
        UserDto user1 = UserDto.builder()
                .name("User One")
                .email("user1@test.com")
                .build();

        String response1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser1 = objectMapper.readValue(response1, UserDto.class);

        UserDto user2 = UserDto.builder()
                .name("User Two")
                .email("user2@test.com")
                .build();

        String response2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto createdUser2 = objectMapper.readValue(response2, UserDto.class);

        UserDto updateDto = UserDto.builder()
                .email("user1@test.com")
                .build();

        mockMvc.perform(patch("/users/{id}", createdUser2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Получение несуществующего пользователя - должно вернуть 404")
    void getNonExistentUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }
}