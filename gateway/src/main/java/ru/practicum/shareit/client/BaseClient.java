package ru.practicum.shareit.client;

import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class BaseClient {
    protected RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    protected ResponseEntity<String> get(String path) {
        return get(path, null, null);
    }

    protected ResponseEntity<String> get(String path, long userId) {
        return get(path, userId, null);
    }

    protected ResponseEntity<String> get(String path, Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, userId, parameters, null);
    }

    protected <T> ResponseEntity<String> post(String path, T body) {
        return post(path, null, null, body);
    }

    protected <T> ResponseEntity<String> post(String path, long userId, T body) {
        return post(path, userId, null, body);
    }

    protected <T> ResponseEntity<String> post(String path, Long userId, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, userId, parameters, body);
    }

    protected <T> ResponseEntity<String> put(String path, long userId, T body) {
        return put(path, userId, null, body);
    }

    protected <T> ResponseEntity<String> put(String path, long userId, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.PUT, path, userId, parameters, body);
    }

    protected <T> ResponseEntity<String> patch(String path, T body) {
        return patch(path, null, null, body);
    }

    protected <T> ResponseEntity<String> patch(String path, long userId) {
        return patch(path, userId, null, null);
    }

    protected <T> ResponseEntity<String> patch(String path, long userId, T body) {
        return patch(path, userId, null, body);
    }

    protected <T> ResponseEntity<String> patch(String path, Long userId, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.PATCH, path, userId, parameters, body);
    }

    protected ResponseEntity<String> delete(String path) {
        return delete(path, null, null);
    }

    protected ResponseEntity<String> delete(String path, long userId) {
        return delete(path, userId, null);
    }

    protected ResponseEntity<String> delete(String path, Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.DELETE, path, userId, parameters, null);
    }

    private <T> ResponseEntity<String> makeAndSendRequest(HttpMethod method, String path, Long userId, @Nullable Map<String, Object> parameters, @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders(userId));

        ResponseEntity<String> shareitServerResponse;
        try {
            if (parameters != null) {
                shareitServerResponse = rest.exchange(path, method, requestEntity, String.class, parameters);
            } else {
                shareitServerResponse = rest.exchange(path, method, requestEntity, String.class);
            }
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
        return prepareGatewayResponse(shareitServerResponse);
    }

    private HttpHeaders defaultHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (userId != null) {
            headers.set("X-Sharer-User-Id", String.valueOf(userId));
        }
        return headers;
    }

    private static ResponseEntity<String> prepareGatewayResponse(ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());

        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }

        return responseBuilder.build();
    }
}
