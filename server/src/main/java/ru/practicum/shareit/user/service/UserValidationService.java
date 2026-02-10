package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.validation.ValidationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

    private final UserRepository userRepository;

    public void validateForCreation(UserDto userDto) {
        log.debug("Валидация пользователя для создания: {}", userDto);

        if (userDto.getId() != null) {
            throw new ValidationException("ID должен быть null при создании");
        }

        validateRequiredFields(userDto);

        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            log.warn("Попытка создания пользователя с уже существующим email={}", userDto.getEmail());
            throw new ValidationException("Пользователь с email=" + userDto.getEmail() + " уже существует");
        }

        log.debug("Валидация для создания прошла успешно");
    }

    public void validateForUpdate(Long id, UserDto userUpdateDto) {
        log.debug("Валидация пользователя для обновления ID={}, данные: {}", id, userUpdateDto);

        if (userUpdateDto.getId() != null && !userUpdateDto.getId().equals(id)) {
            throw new ValidationException("ID в теле запроса не совпадает с ID в пути");
        }

        validateEmailUniqueness(id, userUpdateDto.getEmail());

        log.debug("Валидация для обновления прошла успешно");
    }

    public void validateUserExists(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    private void validateRequiredFields(UserDto userDto) {
        if (userDto.getName() == null || userDto.getName().isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }

        if (!isValidEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Некорректный формат email");
        }
    }

    private void validateEmailUniqueness(Long userId, String email) {
        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            log.warn("Попытка обновления email пользователя с ID={} на уже существующий email={}",
                    userId, email);
            throw new ValidationException("Пользователь с email=" + email + " уже существует");
        }
    }

    boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        // Основные проверки:
        // 1. Есть символ @
        // 2. Есть точка после @
        // 3. После точки есть минимум один символ
        // 4. Нет двух точек подряд
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domainPart = email.substring(atIndex + 1);

        // Проверяем, что в доменной части нет двух точек подряд
        if (domainPart.contains("..")) {
            return false;
        }

        // Проверяем, что есть точка и после последней точки есть символы
        int lastDotIndex = domainPart.lastIndexOf('.');
        return lastDotIndex > 0 && lastDotIndex < domainPart.length() - 1;
    }

    public void validatePaginationParams(int from, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }
    }
}