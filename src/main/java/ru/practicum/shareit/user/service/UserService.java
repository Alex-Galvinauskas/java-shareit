package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    UserDto update(Long id, UserUpdateDto userUpdateDto);

    UserDto getById(Long id);

    List<UserDto> getAll(int from, int size);

    void delete(Long id);
}