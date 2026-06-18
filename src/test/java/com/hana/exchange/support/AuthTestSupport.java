package com.hana.exchange.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public final class AuthTestSupport {

	private static final String PASSWORD = "localPass123!";

	private AuthTestSupport() {
	}

	public static AuthSession signUpAndLogin(MockMvc mockMvc, String username) throws Exception {
		String accountId = signUpAndGetAccountId(mockMvc, username);
		String accessToken = loginAndGetAccessToken(mockMvc, username);
		return new AuthSession(accountId, accessToken);
	}

	public static String signUpAndGetAccountId(MockMvc mockMvc, String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accountId");
	}

	private static String loginAndGetAccessToken(MockMvc mockMvc, String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
	}

	public record AuthSession(String accountId, String accessToken) {

		public String authorizationHeader() {
			return "Bearer " + accessToken;
		}
	}
}
