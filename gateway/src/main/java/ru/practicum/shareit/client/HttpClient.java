package ru.practicum.shareit.client;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import java.util.Map;

public interface HttpClient {

    ResponseEntity<String> get(String path, @Nullable Long userId,
                               @Nullable Map<String, Object> parameters);

    <T> ResponseEntity<String> post(String path, @Nullable Long userId,
                                    @Nullable Map<String, Object> parameters, @Nullable T body);

    <T> ResponseEntity<String> put(String path, @Nullable Long userId,
                                   @Nullable Map<String, Object> parameters, @Nullable T body);

    <T> ResponseEntity<String> patch(String path, @Nullable Long userId,
                                     @Nullable Map<String, Object> parameters, @Nullable T body);

    ResponseEntity<String> delete(String path, @Nullable Long userId, @Nullable Map<String, Object> parameters);
}