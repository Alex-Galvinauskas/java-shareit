package ru.practicum.shareit.client;

import org.springframework.http.ResponseEntity;
import java.util.Map;

public abstract class BaseClient {
    protected final HttpClient httpClient;

    protected BaseClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected ResponseEntity<String> get(String path) {
        return httpClient.get(path, null, null);
    }

    protected ResponseEntity<String> get(String path, Long userId) {
        return httpClient.get(path, userId, null);
    }

    protected ResponseEntity<String> get(String path, Long userId, Map<String, Object> parameters) {
        return httpClient.get(path, userId, parameters);
    }

    protected <T> ResponseEntity<String> post(T body) {
        return httpClient.post("", null, null, body);
    }

    protected <T> ResponseEntity<String> post(String path, Long userId, T body) {
        return httpClient.post(path, userId, null, body);
    }

    protected <T> ResponseEntity<String> patch(String path, T body) {
        return httpClient.patch(path, null, null, body);
    }

    protected <T> ResponseEntity<String> patch(String path, Long userId, T body) {
        return httpClient.patch(path, userId, null, body);
    }

    protected <T> ResponseEntity<String> patch(String path, Long userId, Map<String, Object> parameters) {
        return httpClient.patch(path, userId, parameters, (T) null);
    }

    protected ResponseEntity<String> delete(String path) {
        return httpClient.delete(path, null, null);
    }

}