package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.validation.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        log.info("Создание нового пользователя с email={}", userDto.getEmail());

        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            log.warn("Попытка создания пользователя с уже существующим email={}", userDto.getEmail());
            throw new ValidationException("Пользователь с email=" + userDto.getEmail() + " уже существует");
        }

        User user = userMapper.toEntity(userDto);
        user = userRepository.save(user);

        log.info("Пользователь успешно создан с ID={}: {}", user.getId(), user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserUpdateDto userUpdateDto) {
        log.info("Обновление пользователя с ID={}, новые данные: {}", id, userUpdateDto);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(existingUser.getEmail())) {
            log.debug("Проверка уникальности нового email={} для пользователя с ID={}",
                    userUpdateDto.getEmail(), id);

            if (userRepository.existsByEmailAndIdNot(userUpdateDto.getEmail(), id)) {
                log.warn("Попытка обновления email пользователя с ID={} на уже существующий email={}",
                        id, userUpdateDto.getEmail());
                throw new ValidationException("Пользователь с email=" + userUpdateDto.getEmail() + " уже существует");
            }
        }

        boolean changed = false;

        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(existingUser.getEmail())) {
            log.debug("Обновление email пользователя с ID={}: '{}' -> '{}'",
                    id, existingUser.getEmail(), userUpdateDto.getEmail());

            existingUser.setEmail(userUpdateDto.getEmail());
            changed = true;
        }

        if (userUpdateDto.getName() != null) {
            log.debug("Обновление имени пользователя с ID={}: '{}' -> '{}'",
                    id, existingUser.getName(), userUpdateDto.getName());
            existingUser.setName(userUpdateDto.getName());
            changed = true;
        }

        if (changed) {
            existingUser = userRepository.save(existingUser);
            log.info("Пользователь с ID={} успешно обновлен: {}", id, existingUser);
        } else {
            log.info("Пользователь с ID={} не был изменен, все поля null", id);
        }

        return userMapper.toDto(existingUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        log.debug("Получение пользователя по ID={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        log.debug("Пользователь с ID={} найден: {}", id, user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll(int from, int size) {
        log.debug("Получение списка всех пользователей, from={}, size={}", from, size);

        if (size <= 0) {
            throw new IllegalArgumentException("Размер страницы должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Начальный индекс не может быть отрицательным");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<UserDto> allUsers = userRepository.findAll(pageable).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        log.info("Возвращено {} пользователей", allUsers.size());
        return allUsers;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Удаление пользователя с ID={}", id);

        if (!userRepository.existsById(id)) {
            log.warn("Пользователь с ID={} не найден при попытке удаления", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        userRepository.deleteById(id);
        log.info("Пользователь с ID={} успешно удален", id);
    }
}