package com.hana.exchange.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiDocsExposeBusinessApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title", equalTo("Stock Exchange BE API")))
				.andExpect(jsonPath("$.paths['/api/v1/auth/signup']", notNullValue()))
				.andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}']", notNullValue()))
				.andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/deposits']", notNullValue()))
				.andExpect(jsonPath("$.paths['/api/v1/market/quotes']", notNullValue()))
				.andExpect(jsonPath("$.paths['/api/v1/market/quotes/{stockCode}']", notNullValue()));
	}
}
