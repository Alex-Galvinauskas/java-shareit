package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.dto.ItemRequestDto;

import java.util.Map;

@Service
public class ItemRequestClient extends BaseClient {
    private static final String API_PREFIX = "/requests";

    public ItemRequestClient(@Value("${shareit-server.url}") String serverUrl,
                             HttpClientFactory httpClientFactory) {
        super(httpClientFactory.createClient(serverUrl, API_PREFIX));
    }

    public ResponseEntity<String> create(long userId, ItemRequestDto itemRequestDto) {
        return post("", userId, itemRequestDto);
    }

    public ResponseEntity<String> getOwnRequests(long userId) {
        return get("", userId);
    }

    public ResponseEntity<String> getOtherUsersRequests(long userId, Integer from, Integer size) {
        Map<String, Object> parameters = Map.of(
                "from", from,
                "size", size
        );
        return get("/all?from={from}&size={size}", userId, parameters);
    }

    public ResponseEntity<String> getById(long userId, Long requestId) {
        return get("/" + requestId, userId);
    }
}