package ru.practicum.shareit.client;

import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class RestHttpClient implements HttpClient {
    private final RestTemplate rest;

    public RestHttpClient(RestTemplate rest) {
        this.rest = rest;
    }

    @Override
    public ResponseEntity<String> get(String path, @Nullable Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, userId, parameters, null);
    }

    @Override
    public <T> ResponseEntity<String> post(String path, @Nullable Long userId, @Nullable Map<String,
            Object> parameters, @Nullable T body) {
        return makeAndSendRequest(HttpMethod.POST, path, userId, parameters, body);
    }

    @Override
    public <T> ResponseEntity<String> put(String path, @Nullable Long userId, @Nullable Map<String,
            Object> parameters, @Nullable T body) {
        return makeAndSendRequest(HttpMethod.PUT, path, userId, parameters, body);
    }

    @Override
    public <T> ResponseEntity<String> patch(String path, @Nullable Long userId, @Nullable Map<String,
            Object> parameters, @Nullable T body) {
        return makeAndSendRequest(HttpMethod.PATCH, path, userId, parameters, body);
    }

    @Override
    public ResponseEntity<String> delete(String path, @Nullable Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.DELETE, path, userId, parameters, null);
    }

    private <T> ResponseEntity<String> makeAndSendRequest(HttpMethod method, String path, Long userId,
                                                          @Nullable Map<String, Object> parameters, @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders(userId));

        ResponseEntity<String> response;
        try {
            if (parameters != null) {
                response = rest.exchange(path, method, requestEntity, String.class,
                        parameters);
            } else {
                response = rest.exchange(path, method, requestEntity, String.class);
            }
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
        return prepareResponse(response);
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

    private static ResponseEntity<String> prepareResponse(ResponseEntity<String> response) {
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