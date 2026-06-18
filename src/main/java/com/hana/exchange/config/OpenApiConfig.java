package com.hana.exchange.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI stockExchangeOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Stock Exchange BE API")
						.version("v1")
						.description("Partner exchange API for English/USD Korean stock market app."));
	}
}
