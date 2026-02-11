package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса пользователей")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserValidationService validationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Создание пользователя - успешно")
    void createUser_shouldCreateAndReturnUser() {
        UserDto userDto = UserDto.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        UserDto savedUserDto = UserDto.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(savedUserDto);

        UserDto result = userService.create(userDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());

        verify(validationService).validateForCreation(userDto);
        verify(userMapper).toEntity(userDto);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Обновление пользователя - успешно")
    void updateUser_shouldUpdateAndReturnUser() {
        Long userId = 1L;
        UserDto updateDto = UserDto.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        User existingUser = User.builder()
                .id(userId)
                .name("Old Name")
                .email("old@example.com")
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        UserDto resultDto = UserDto.builder()
                .id(userId)
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(resultDto);

        UserDto result = userService.update(userId, updateDto);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Updated Name", result.getName());
        assertEquals("updated@example.com", result.getEmail());

        verify(validationService).validateForUpdate(userId, updateDto);
        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    @DisplayName("Обновление пользователя - только имя")
    void updateUser_onlyName_shouldUpdateOnlyName() {
        Long userId = 1L;
        UserDto updateDto = UserDto.builder()
                .name("Updated Name")
                .build(); // email не указан

        User existingUser = User.builder()
                .id(userId)
                .name("Old Name")
                .email("old@example.com")
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .name("Updated Name")
                .email("old@example.com")
                .build();

        UserDto resultDto = UserDto.builder()
                .id(userId)
                .name("Updated Name")
                .email("old@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(resultDto);

        UserDto result = userService.update(userId, updateDto);

        assertEquals("Updated Name", result.getName());
        assertEquals("old@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Обновление пользователя - только email")
    void updateUser_onlyEmail_shouldUpdateOnlyEmail() {
        Long userId = 1L;
        UserDto updateDto = UserDto.builder()
                .email("updated@example.com")
                .build();

        User existingUser = User.builder()
                .id(userId)
                .name("Старое имя")
                .email("old@example.com")
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .name("Старое имя")
                .email("updated@example.com")
                .build();

        UserDto resultDto = UserDto.builder()
                .id(userId)
                .name("Старое имя")
                .email("updated@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(resultDto);

        UserDto result = userService.update(userId, updateDto);

        assertEquals("Старое имя", result.getName());
        assertEquals("updated@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя - должно выбросить исключение")
    void updateUser_nonExistentUser_shouldThrowNotFoundException() {
        Long userId = 999L;
        UserDto updateDto = UserDto.builder()
                .name("Updated Name")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.update(userId, updateDto));

        assertEquals("Пользователь с id=" + userId + " не найден", exception.getMessage());
        verify(validationService).validateForUpdate(userId, updateDto);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение пользователя по ID - успешно")
    void getUserById_shouldReturnUser() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .build();

        UserDto userDto = UserDto.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.getById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Получение несуществующего пользователя по ID - должно выбросить исключение")
    void getUserById_nonExistentUser_shouldThrowNotFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getById(userId));

        assertEquals("Пользователь с id=" + userId + " не найден", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Получение сущности пользователя по ID - успешно")
    void getUserEntityById_shouldReturnUserEntity() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserEntityById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Получение сущности несуществующего пользователя - должно выбросить исключение")
    void getUserEntityById_nonExistentUser_shouldThrowNotFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getUserEntityById(userId));

        assertEquals("Пользователь с id=" + userId + " не найден", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Получение пользователей по списку ID - успешно")
    void getUsersByIds_shouldReturnUsers() {
        List<Long> userIds = List.of(1L, 2L, 3L);
        List<User> users = List.of(
                User.builder().id(1L).name("User1").build(),
                User.builder().id(2L).name("User2").build(),
                User.builder().id(3L).name("User3").build()
        );

        when(userRepository.findByIdIn(userIds)).thenReturn(users);

        List<User> result = userService.getUsersByIds(userIds);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(userIds.getFirst(), result.getFirst().getId());

        verify(userRepository).findByIdIn(userIds);
    }

    @Test
    @DisplayName("Получение пользователей по пустому списку ID - должен вернуть пустой список")
    void getUsersByIds_emptyList_shouldReturnEmptyList() {
        List<User> result = userService.getUsersByIds(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByIdIn(any());
    }

    @Test
    @DisplayName("Получение пользователей по null списку ID - должен вернуть пустой список")
    void getUsersByIds_nullList_shouldReturnEmptyList() {
        List<User> result = userService.getUsersByIds(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByIdIn(any());
    }

    @Test
    @DisplayName("Получение всех пользователей с пагинацией - успешно")
    void getAllUsers_shouldReturnPaginatedUsers() {
        int from = 0;
        int size = 10;

        List<User> users = List.of(
                User.builder().id(1L).name("User1").email("user1@example.com").build(),
                User.builder().id(2L).name("User2").email("user2@example.com").build()
        );

        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(from / size, size,
                Sort.by("id").ascending());

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDto(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    return UserDto.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .build();
                });

        List<UserDto> result = userService.getAll(from, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        verify(validationService).validatePaginationParams(from, size);
        verify(userRepository).findAll(pageable);
        verify(userMapper, times(2)).toDto(any(User.class));
    }

    @Test
    @DisplayName("Получение всех пользователей со второй страницы")
    void getAllUsers_secondPage_shouldReturnCorrectUsers() {
        int from = 10;
        int size = 5;
        Pageable pageable = PageRequest.of(from / size, size,
                Sort.by("id").ascending());

        when(userRepository.findAll(pageable)).thenReturn(Page.empty());

        List<UserDto> result = userService.getAll(from, size);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Удаление пользователя - успешно")
    void deleteUser_shouldDeleteUser() {
        Long userId = 1L;

        doNothing().when(validationService).validateUserExists(userId);
        doNothing().when(userRepository).deleteById(userId);

        assertDoesNotThrow(() -> userService.delete(userId));

        verify(validationService).validateUserExists(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("Удаление несуществующего пользователя - должно выбросить исключение")
    void deleteUser_nonExistentUser_shouldThrowNotFoundException() {
        Long userId = 999L;

        doThrow(new NotFoundException("Пользователь с id=" + userId + " не найден"))
                .when(validationService).validateUserExists(userId);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.delete(userId));

        assertEquals("Пользователь с id=" + userId + " не найден", exception.getMessage());
        verify(validationService).validateUserExists(userId);
        verify(userRepository, never()).deleteById(anyLong());
    }
}