package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты граничных случаев сервиса пользователей")
class UserServiceEdgeCasesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserValidationService validationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Создание пользователя с минимальными данными")
    void createUser_withMinimalData_shouldCreateSuccessfully() {
        UserDto userDto = UserDto.builder()
                .name("A")
                .email("a@b.c")
                .build();

        User user = User.builder()
                .id(1L)
                .name("A")
                .email("a@b.c")
                .build();

        UserDto savedDto = UserDto.builder()
                .id(1L)
                .name("A")
                .email("a@b.c")
                .build();

        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(savedDto);

        UserDto result = userService.create(userDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("A", result.getName());
        assertEquals("a@b.c", result.getEmail());
    }

    @Test
    @DisplayName("Получение всех пользователей - пустая база данных")
    void getAllUsers_emptyDatabase_shouldReturnEmptyList() {
        int from = 0;
        int size = 10;

        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        List<UserDto> result = userService.getAll(from, size);

        assertNotNull(result, "Результат не должен быть null");
        assertTrue(result.isEmpty(), "При пустой БД должен вернуться пустой список");

        assertDoesNotThrow(() -> userService.getAll(from, size),
                "Метод не должен выбрасывать исключения при пустой БД");
    }

    @Test
    @DisplayName("Получение пользователей по ID - один из ID не существует")
    void getUsersByIds_someIdsMissing_shouldReturnOnlyExistingUsers() {
        List<Long> userIds = List.of(1L, 999L, 3L);
        List<User> existingUsers = List.of(
                User.builder().id(1L).name("User1").build(),
                User.builder().id(3L).name("User3").build()
        );

        when(userRepository.findByIdIn(userIds)).thenReturn(existingUsers);

        List<User> result = userService.getUsersByIds(userIds);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(1L)));
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(3L)));
        assertTrue(result.stream().noneMatch(u -> u.getId().equals(999L)));
    }

    @Test
    @DisplayName("Обновление пользователя - пустой DTO (без изменений)")
    void updateUser_emptyDto_shouldReturnSameUser() {
        Long userId = 1L;
        UserDto emptyDto = UserDto.builder().build();

        User existingUser = User.builder()
                .id(userId)
                .name("Original Name")
                .email("original@email.com")
                .build();

        UserDto resultDto = UserDto.builder()
                .id(userId)
                .name("Original Name")
                .email("original@email.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toDto(existingUser)).thenReturn(resultDto);

        UserDto result = userService.update(userId, emptyDto);

        assertEquals("Original Name", result.getName());
        assertEquals("original@email.com", result.getEmail());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Получение пользователей с большой страницей")
    void getAllUsers_largePageSize_shouldHandleCorrectly() {
        int from = 0;
        int size = 1000;

        List<User> users = Collections.nCopies(1000, User.builder().id(1L).build());
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toDto(any(User.class)))
                .thenReturn(UserDto.builder().id(1L).build());

        List<UserDto> result = userService.getAll(from, size);

        assertEquals(1000, result.size());
        verify(validationService).validatePaginationParams(from, size);
        verify(userRepository).findAll(any(Pageable.class));
    }
}