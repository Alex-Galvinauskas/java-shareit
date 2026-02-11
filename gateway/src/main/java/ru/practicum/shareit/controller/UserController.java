package ru.practicum.shareit.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.client.UserClient;
import ru.practicum.shareit.dto.UserDto;

@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
public class UserController {
    private final UserClient userClient;

    @PostMapping
    public ResponseEntity<String> create(@Validated(UserDto.OnCreate.class) @RequestBody UserDto userDto) {
        return userClient.create(userDto);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id,
                                         @Validated(UserDto.OnUpdate.class) @RequestBody UserDto userUpdateDto) {
        return userClient.update(id, userUpdateDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getById(@PathVariable Long id) {
        return userClient.getById(id);
    }

    @GetMapping
    public ResponseEntity<String> getAll(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return userClient.getAll(from, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return userClient.delete(id);
    }
}