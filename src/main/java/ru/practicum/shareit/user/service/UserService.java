package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    UserDto update(Long id, UserDto userUpdateDto);

    UserDto getById(Long id);

    User getUserEntityById(Long id);

    List<User> getUsersByIds(List<Long> ids);

    List<UserDto> getAll(int from, int size);

    void delete(Long id);
}