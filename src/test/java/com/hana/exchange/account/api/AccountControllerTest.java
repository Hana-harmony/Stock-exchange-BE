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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
	void depositUsdIncreasesMockCashBalance() throws Exception {
		String accountId = signUpAndGetAccountId("DepositTrader01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": 125.50
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.currency").value("USD"))
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("125.50"))
				.andExpect(jsonPath("$.data.lastLedgerEntryId", notNullValue()));

		mockMvc.perform(get("/api/v1/accounts/{accountId}", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("125.50"))
				.andExpect(jsonPath("$.data.lastLedgerEntryId").doesNotExist());
	}

	@Test
	void depositRejectsUnknownAccount() throws Exception {
		mockMvc.perform(post("/api/v1/accounts/ACC-UNKNOWN00000/deposits")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": 10.00
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("ACCOUNT_001"));
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
