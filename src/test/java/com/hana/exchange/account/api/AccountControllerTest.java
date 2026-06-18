package com.hana.exchange.account.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void signUpCreatesMockUsdAccount() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "LocalTrader01",
								  "password": "localPass123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.code").value("COMMON_000"))
				.andExpect(jsonPath("$.data.userId", notNullValue()))
				.andExpect(jsonPath("$.data.username").value("localtrader01"))
				.andExpect(jsonPath("$.data.accountId", notNullValue()))
				.andExpect(jsonPath("$.data.currency").value("USD"))
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("0.00"))
				.andExpect(jsonPath("$.data.tradingMode").value("EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING"));
	}

	@Test
	void signUpRejectsDuplicateUsername() throws Exception {
		String payload = """
				{
				  "username": "DuplicateTrader",
				  "password": "localPass123!"
				}
				""";
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_001"));
	}

	@Test
	void loginIssuesBearerTokenAndVerifyTokenReturnsClaims() throws Exception {
		String accountId = signUpAndGetAccountId("LoginTrader01");
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "LoginTrader01",
								  "password": "localPass123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value("logintrader01"))
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.accessToken", notNullValue()))
				.andExpect(jsonPath("$.data.refreshToken", notNullValue()))
				.andExpect(jsonPath("$.data.sessionId", notNullValue()))
				.andExpect(jsonPath("$.data.issuedAt", notNullValue()))
				.andExpect(jsonPath("$.data.expiresAt", notNullValue()))
				.andExpect(jsonPath("$.data.refreshTokenExpiresAt", notNullValue()))
				.andReturn();

		String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
		mockMvc.perform(post("/api/v1/auth/token/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "accessToken": "%s"
								}
								""".formatted(accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.valid").value(true))
				.andExpect(jsonPath("$.data.username").value("logintrader01"))
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.issuedAt", notNullValue()))
				.andExpect(jsonPath("$.data.expiresAt", notNullValue()));
	}

	@Test
	void refreshTokenRotatesSessionAndRejectsOldRefreshToken() throws Exception {
		signUpAndGetAccountId("RefreshTrader01");
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "RefreshTrader01",
								  "password": "localPass123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");
		String firstSessionId = JsonPath.read(login.getResponse().getContentAsString(), "$.data.sessionId");
		MvcResult refresh = mockMvc.perform(post("/api/v1/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value("refreshtrader01"))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.accessToken", notNullValue()))
				.andExpect(jsonPath("$.data.refreshToken", notNullValue()))
				.andExpect(jsonPath("$.data.sessionId", notNullValue()))
				.andExpect(jsonPath("$.data.refreshTokenExpiresAt", notNullValue()))
				.andReturn();

		String secondSessionId = JsonPath.read(refresh.getResponse().getContentAsString(), "$.data.sessionId");
		org.assertj.core.api.Assertions.assertThat(secondSessionId).isNotEqualTo(firstSessionId);
		mockMvc.perform(post("/api/v1/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_005"));
	}

	@Test
	void logoutRevokesRefreshSession() throws Exception {
		signUpAndGetAccountId("LogoutTrader01");
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "LogoutTrader01",
								  "password": "localPass123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");
		String sessionId = JsonPath.read(login.getResponse().getContentAsString(), "$.data.sessionId");

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.revoked").value(true))
				.andExpect(jsonPath("$.data.sessionId").value(sessionId))
				.andExpect(jsonPath("$.data.revokedAt", notNullValue()));

		mockMvc.perform(post("/api/v1/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "%s"
								}
								""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_005"));
	}

	@Test
	void loginRejectsInvalidPassword() throws Exception {
		signUpAndGetAccountId("BadLoginTrader01");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "BadLoginTrader01",
								  "password": "wrongPass123!"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_002"));
	}

	@Test
	void verifyTokenRejectsTamperedToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/token/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "accessToken": "bad.token.value"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_003"));
	}

	@Test
	void depositUsdIncreasesMockCashBalance() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "DepositTrader01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": 125.50
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accountId").value(session.accountId()))
				.andExpect(jsonPath("$.data.currency").value("USD"))
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("125.50"))
				.andExpect(jsonPath("$.data.lastLedgerEntryId", notNullValue()));

		mockMvc.perform(get("/api/v1/accounts/{accountId}", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("125.50"))
				.andExpect(jsonPath("$.data.lastLedgerEntryId").doesNotExist());
	}

	@Test
	void accountApiRejectsMissingBearerToken() throws Exception {
		mockMvc.perform(post("/api/v1/accounts/ACC-UNKNOWN00000/deposits")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": 10.00
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_003"));
	}

	@Test
	void accountApiRejectsDifferentAccountToken() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "ForbiddenTrader01");

		mockMvc.perform(get("/api/v1/accounts/ACC-UNKNOWN00000")
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_004"));
	}

	@Test
	void accountApiRejectsInvalidInput() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "bad",
								  "password": "short"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private String signUpAndGetAccountId(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "localPass123!"
								}
								""".formatted(username)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accountId");
	}
}
