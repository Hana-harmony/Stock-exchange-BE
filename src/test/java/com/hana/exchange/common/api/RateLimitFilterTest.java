package com.hana.exchange.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"exchange.rate-limit.enabled=true",
		"exchange.rate-limit.max-requests=2",
		"exchange.rate-limit.window=1m"
})
class RateLimitFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rateLimitReturnsCommonErrorAfterConfiguredRequestCount() throws Exception {
		mockMvc.perform(signup("RateLimit01"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Limit", "2"))
				.andExpect(header().string("X-RateLimit-Remaining", "1"));
		mockMvc.perform(signup("RateLimit02"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Limit", "2"))
				.andExpect(header().string("X-RateLimit-Remaining", "0"));

		mockMvc.perform(signup("RateLimit03"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("X-RateLimit-Limit", "2"))
				.andExpect(header().string("X-RateLimit-Remaining", "0"))
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.code").value("COMMON_004"));
	}

	private org.springframework.test.web.servlet.RequestBuilder signup(String username) {
		return post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "%s",
						  "password": "strong-password"
						}
						""".formatted(username));
	}
}
