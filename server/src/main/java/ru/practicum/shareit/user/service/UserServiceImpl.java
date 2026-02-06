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
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserValidationService validationService;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        log.info("Создание нового пользователя с email={}", userDto.getEmail());

        validationService.validateForCreation(userDto);

        User user = userMapper.toEntity(userDto);
        user = userRepository.save(user);

        log.info("Пользователь успешно создан с ID={}, email={}", user.getId(), user.getEmail());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto userUpdateDto) {
        log.info("Обновление пользователя с ID={}", id);

        validationService.validateForUpdate(id, userUpdateDto);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        updateUserFields(existingUser, userUpdateDto);

        User updatedUser = userRepository.save(existingUser);
        log.info("Пользователь с ID={} успешно обновлен", id);

        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        log.debug("Получение пользователя по ID={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        log.debug("Пользователь с ID={} найден: name={}", id, user.getName());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        log.debug("Получение сущности пользователя по ID={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        log.debug("Сущность пользователя с ID={} найдена", id);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByIds(List<Long> ids) {
        log.debug("Получение списка пользователей по IDs ({} элементов)", ids != null ? ids.size() : 0);

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findByIdIn(ids);
        log.debug("Найдено {} пользователей", users.size());
        return users;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll(int from, int size) {
        log.debug("Получение списка всех пользователей, from={}, size={}", from, size);

        validationService.validatePaginationParams(from, size);

        Pageable pageable = PageRequest.of(from / size, size,
                Sort.by("id").ascending());
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

        validationService.validateUserExists(id);
        userRepository.deleteById(id);

        log.info("Пользователь с ID={} успешно удален", id);
    }

    private void updateUserFields(User user, UserDto userUpdateDto) {
        Optional.ofNullable(userUpdateDto.getEmail())
                .filter(email -> !email.equals(user.getEmail()))
                .ifPresent(user::setEmail);

        Optional.ofNullable(userUpdateDto.getName())
                .filter(name -> !name.equals(user.getName()))
                .ifPresent(user::setName);
    }
}