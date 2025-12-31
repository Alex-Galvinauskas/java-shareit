package ru.practicum.shareit.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;
import ru.practicum.shareit.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(UserDto userDto);

    @Mapping(target = "id", ignore = true)
    User toEntity(UserUpdateDto userUpdateDto);

    @Mapping(target = "id", ignore = true)
    UserDto toDto(UserUpdateDto userUpdateDto);

    UserUpdateDto toUpdateDto(UserDto userDto);
}