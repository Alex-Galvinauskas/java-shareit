package ru.practicum.shareit.client;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Component
public class HttpClientFactory {

    public HttpClient createClient(String serverUrl, String apiPrefix) {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + apiPrefix))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();

        return new RestHttpClient(restTemplate);
    }
}