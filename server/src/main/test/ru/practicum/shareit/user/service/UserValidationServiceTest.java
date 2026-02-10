package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.validation.ValidationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Тесты сервиса валидации пользователей")
@ExtendWith(MockitoExtension.class)
class UserValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserValidationService userValidationService;

    private UserDto userDto;

    @BeforeEach
    void setUp() {

        userDto = UserDto.builder()
                .id(null)
                .name("Тестовый пользователь")
                .email("test@email.com")
                .build();

    }

    @Test
    @DisplayName("Валидация для создания при валидном пользователе не должна выбрасывать исключение")
    void validateForCreation_whenValidUser_shouldNotThrow() {

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userValidationService.validateForCreation(userDto));

    }

    @Test
    @DisplayName("Валидация для создания при не null ID должна выбрасывать исключение валидации")
    void validateForCreation_whenIdNotNull_shouldThrowValidationException() {

        userDto.setId(1L);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userValidationService.validateForCreation(userDto));

        assertEquals("ID должен быть null при создании", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для создания при пустом имени должна выбрасывать исключение")
    void validateForCreation_whenNameBlank_shouldThrowIllegalArgumentException() {

        userDto.setName(" ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userValidationService.validateForCreation(userDto));

        assertEquals("Имя не может быть пустым", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для создания при пустом email должна выбрасывать исключение")
    void validateForCreation_whenEmailBlank_shouldThrowIllegalArgumentException() {

        userDto.setEmail(" ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userValidationService.validateForCreation(userDto));

        assertEquals("Email не может быть пустым", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для создания при невалидном формате email должна выбрасывать исключение")
    void validateForCreation_whenInvalidEmailFormat_shouldThrowIllegalArgumentException() {

        userDto.setEmail("невалидный-email");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userValidationService.validateForCreation(userDto));

        assertEquals("Некорректный формат email", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для создания при существующем email должна выбрасывать исключение валидации")
    void validateForCreation_whenEmailAlreadyExists_shouldThrowValidationException() {

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(
                ru.practicum.shareit.user.model.User.builder().id(1L).build()
        ));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userValidationService.validateForCreation(userDto));

        assertEquals("Пользователь с email=test@email.com уже существует", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для обновления при валидном обновлении не должна выбрасывать исключение")
    void validateForUpdate_whenValidUpdate_shouldNotThrow() {

        UserDto updateDto = UserDto.builder()
                .id(1L)
                .email("new@email.com")
                .build();

        when(userRepository.existsByEmailAndIdNot("new@email.com", 1L)).thenReturn(false);

        assertDoesNotThrow(() -> userValidationService.validateForUpdate(1L, updateDto));

    }

    @Test
    @DisplayName("Валидация для обновления при несовпадении ID должна выбрасывать исключение валидации")
    void validateForUpdate_whenIdMismatch_shouldThrowValidationException() {

        UserDto updateDto = UserDto.builder()
                .id(2L)
                .email("new@email.com")
                .build();

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userValidationService.validateForUpdate(1L, updateDto));

        assertEquals("ID в теле запроса не совпадает с ID в пути", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для обновления при существующем email у другого пользователя должна выбрасывать исключение")
    void validateForUpdate_whenEmailAlreadyExistsForOtherUser_shouldThrowValidationException() {

        UserDto updateDto = UserDto.builder()
                .id(1L)
                .email("existing@email.com")
                .build();

        when(userRepository.existsByEmailAndIdNot("existing@email.com", 1L)).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userValidationService.validateForUpdate(1L, updateDto));

        assertEquals("Пользователь с email=existing@email.com уже существует", exception.getMessage());

    }

    @Test
    @DisplayName("Валидация для обновления при null email не должна проверять уникальность")
    void validateForUpdate_whenEmailIsNull_shouldNotCheckUniqueness() {

        UserDto updateDto = UserDto.builder()
                .id(1L)
                .name("Новое имя")
                .build();

        assertDoesNotThrow(() -> userValidationService.validateForUpdate(1L, updateDto));

        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), any());
    }

    @Test
    @DisplayName("Валидация существования пользователя при существовании пользователя не должна выбрасывать исключение")
    void validateUserExists_whenUserExists_shouldNotThrow() {

        when(userRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> userValidationService.validateUserExists(1L));

    }

    @Test
    @DisplayName("Валидация существования пользователя при отсутствии пользователя должна выбрасывать исключение")
    void validateUserExists_whenUserNotExists_shouldThrowNotFoundException() {

        when(userRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userValidationService.validateUserExists(1L));

        assertEquals("Пользователь с id=1 не найден", exception.getMessage());

    }

    @Test
    @DisplayName("Проверка валидности email для различных email должна возвращать корректные результаты")
    void isValidEmail_whenVariousEmails_shouldReturnCorrectResults() {

        assertFalse(userValidationService.isValidEmail(null));

        assertFalse(userValidationService.isValidEmail("plainaddress"));

        assertFalse(userValidationService.isValidEmail("@domain.com"));

        assertFalse(userValidationService.isValidEmail("email@domain"));

        assertTrue(userValidationService.isValidEmail("email@domain.com"));

        assertTrue(userValidationService.isValidEmail("firstname.lastname@domain.com"));

        assertTrue(userValidationService.isValidEmail("email@subdomain.domain.com"));

    }

    @Test
    @DisplayName("Валидация параметров пагинации с валидными параметрами не должна выбрасывать исключение")
    void validatePaginationParams_whenValidParams_shouldNotThrow() {

        assertDoesNotThrow(() -> userValidationService.validatePaginationParams(0, 10));

        assertDoesNotThrow(() -> userValidationService.validatePaginationParams(100, 50));

    }

    @Test
    @DisplayName("Валидация параметров пагинации с невалидными параметрами должна выбрасывать исключение")
    void validatePaginationParams_whenInvalidParams_shouldThrow() {

        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
                () -> userValidationService.validatePaginationParams(-1, 10));

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
                () -> userValidationService.validatePaginationParams(0, 0));

        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
                () -> userValidationService.validatePaginationParams(0, -5));

    }

    @Test
    @DisplayName("Пограничные случаи валидации email")
    void emailValidationEdgeCases() {

        UserValidationService validationService = new UserValidationService(null);

        assertTrue(validationService.isValidEmail("test@example.com"));

        assertTrue(validationService.isValidEmail("user.name@domain.co.uk"));

        assertTrue(validationService.isValidEmail("user+tag@example.com"));

        assertTrue(validationService.isValidEmail("agalvinauskas@yandex.ru"));

        assertFalse(validationService.isValidEmail(null));

        assertFalse(validationService.isValidEmail(""));

        assertFalse(validationService.isValidEmail("plainaddress"));

        assertFalse(validationService.isValidEmail("@domain.com"));

        assertFalse(validationService.isValidEmail("email@"));

        assertFalse(validationService.isValidEmail("email@domain."));

        assertFalse(validationService.isValidEmail("email@.com"));

        assertFalse(validationService.isValidEmail("@."));

        assertFalse(validationService.isValidEmail("email@domain..com"));

    }
}