package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.validation.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<String, Long> emailToIdMap = new HashMap<>();
    private final AtomicLong currentId = new AtomicLong(1L);
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserDto userDto) {
        log.info("Создание нового пользователя с email={}", userDto.getEmail());

        if (emailToIdMap.containsKey(userDto.getEmail())) {
            log.warn("Попытка создания пользователя с уже существующим email={}", userDto.getEmail());
            throw new ValidationException("Пользователь с email=" + userDto.getEmail() + " уже существует");
        }

        User user = userMapper.toEntity(userDto);
        user.setId(currentId.getAndIncrement());
        users.put(user.getId(), user);
        emailToIdMap.put(user.getEmail(), user.getId());

        log.info("Пользователь успешно создан с ID={}: {}", user.getId(), user);
        return userMapper.toDto(user);
    }

    @Override
    public UserDto update(Long id, UserUpdateDto userUpdateDto) {
        log.info("Обновление пользователя с ID={}, новые данные: {}", id, userUpdateDto);

        User existingUser = users.get(id);
        if (existingUser == null) {
            log.warn("Пользователь с ID={} не найден при попытке обновления", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(existingUser.getEmail())) {
            log.debug("Проверка уникальности нового email={} для пользователя с ID={}",
                    userUpdateDto.getEmail(), id);

            Long existingUserIdWithEmail = emailToIdMap.get(userUpdateDto.getEmail());
            if (existingUserIdWithEmail != null && !existingUserIdWithEmail.equals(id)) {
                log.warn("Попытка обновления email пользователя с ID={} на уже существующий email={}",
                        id, userUpdateDto.getEmail());
                throw new ValidationException("Пользователь с email=" + userUpdateDto.getEmail() + " уже существует");
            }
        }

        boolean changed = false;

        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(existingUser.getEmail())) {
            log.debug("Обновление email пользователя с ID={}: '{}' -> '{}'",
                    id, existingUser.getEmail(), userUpdateDto.getEmail());

            emailToIdMap.remove(existingUser.getEmail());
            existingUser.setEmail(userUpdateDto.getEmail());
            emailToIdMap.put(userUpdateDto.getEmail(), id);
            changed = true;
        }

        if (userUpdateDto.getName() != null) {
            log.debug("Обновление имени пользователя с ID={}: '{}' -> '{}'",
                    id, existingUser.getName(), userUpdateDto.getName());
            existingUser.setName(userUpdateDto.getName());
            changed = true;
        }

        if (changed) {
            users.put(id, existingUser);
            log.info("Пользователь с ID={} успешно обновлен: {}", id, existingUser);
        } else {
            log.info("Пользователь с ID={} не был изменен, все поля null", id);
        }

        return userMapper.toDto(existingUser);
    }

    @Override
    public UserDto getById(Long id) {
        log.debug("Получение пользователя по ID={}", id);

        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь с ID={} не найден", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        log.debug("Пользователь с ID={} найден: {}", id, user);
        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        log.debug("Получение списка всех пользователей");
        List<UserDto> allUsers = new ArrayList<>();
        for (User user : users.values()) {
            allUsers.add(userMapper.toDto(user));
        }
        log.info("Возвращено {} пользователей", allUsers.size());
        return allUsers;
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление пользователя с ID={}", id);

        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь с ID={} не найден при попытке удаления", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        emailToIdMap.remove(user.getEmail());
        users.remove(id);

        log.info("Пользователь с ID={} успешно удален", id);
    }
}