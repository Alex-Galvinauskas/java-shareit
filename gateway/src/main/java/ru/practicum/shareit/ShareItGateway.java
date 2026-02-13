package ru.practicum.shareit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.practicum.shareit.client.HttpClientFactory;

@SpringBootApplication
public class ShareItGateway {
	public static void main(String[] args) {
		SpringApplication.run(ShareItGateway.class, args);
	}

	@Bean
	public HttpClientFactory httpClientFactory() {
		return new HttpClientFactory();
	}

}
