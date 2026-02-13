package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.dto.UserDto;

import java.util.Map;

@Service
public class UserClient extends BaseClient {
    private static final String API_PREFIX = "/users";

    public UserClient(@Value("${shareit-server.url}") String serverUrl,
                      HttpClientFactory httpClientFactory) {
        super(httpClientFactory.createClient(serverUrl, API_PREFIX));
    }

    public ResponseEntity<String> create(UserDto userDto) {
        return post(userDto);
    }

    public ResponseEntity<String> update(long userId, UserDto userDto) {
        return patch("/" + userId, userDto);
    }

    public ResponseEntity<String> getById(long userId) {
        return get("/" + userId);
    }

    public ResponseEntity<String> getAll(Integer from, Integer size) {
        Map<String, Object> parameters = Map.of(
                "from", from,
                "size", size
        );
        return get("?from={from}&size={size}", null, parameters);
    }

    public ResponseEntity<String> delete(long userId) {
        return delete("/" + userId);
    }
}