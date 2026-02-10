package ru.practicum.shareit.user.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты маппера пользователей")
class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Маппинг User в UserDto")
    void toDto_shouldMapUserToUserDto() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        UserDto userDto = userMapper.toDto(user);

        assertNotNull(userDto);
        assertEquals(user.getId(), userDto.getId());
        assertEquals(user.getName(), userDto.getName());
        assertEquals(user.getEmail(), userDto.getEmail());
    }

    @Test
    @DisplayName("Маппинг UserDto в User")
    void toEntity_shouldMapUserDtoToUser() {
        UserDto userDto = UserDto.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        User user = userMapper.toEntity(userDto);

        assertNotNull(user);
        assertEquals(userDto.getId(), user.getId());
        assertEquals(userDto.getName(), user.getName());
        assertEquals(userDto.getEmail(), user.getEmail());
    }

    @Test
    @DisplayName("Маппинг null User в null UserDto")
    void toDto_withNullUser_shouldReturnNull() {
        assertNull(userMapper.toDto(null));
    }

    @Test
    @DisplayName("Маппинг null UserDto в null User")
    void toEntity_withNullUserDto_shouldReturnNull() {
        assertNull(userMapper.toEntity(null));
    }

    @Test
    @DisplayName("Маппинг User с частичными данными")
    void toDto_withPartialData_shouldMapCorrectly() {
        User user = User.builder()
                .id(1L)
                .name(null)
                .email("test@example.com")
                .build();

        UserDto userDto = userMapper.toDto(user);

        assertNotNull(userDto);
        assertEquals(1L, userDto.getId());
        assertNull(userDto.getName());
        assertEquals("test@example.com", userDto.getEmail());
    }
}