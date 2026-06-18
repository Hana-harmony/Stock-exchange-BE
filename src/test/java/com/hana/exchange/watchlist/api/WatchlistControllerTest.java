package com.hana.exchange.watchlist.api;

import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;

@SpringBootTest
@AutoConfigureMockMvc
class WatchlistControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void getWatchlistReturnsEmptyItemsForNewAccount() throws Exception {
		String accountId = signUpAndGetAccountId("WatchEmpty01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/watchlist", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.itemCount").value(0))
				.andExpect(jsonPath("$.data.targetingMode").value("WATCHLIST_ALERT_TARGET"))
				.andExpect(jsonPath("$.data.items", empty()));
	}

	@Test
	void addWatchlistItemUsesOmniLensMetadataAndIsIdempotent() throws Exception {
		String accountId = signUpAndGetAccountId("WatchAdd01");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "KOSPI"));

		addStock(accountId, "005930")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemCount").value(1))
				.andExpect(jsonPath("$.data.items[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.items[0].stockName").value("Samsung Electronics"))
				.andExpect(jsonPath("$.data.items[0].market").value("KOSPI"))
				.andExpect(jsonPath("$.data.items[0].targetingMode").value("WATCHLIST_ALERT_TARGET"));

		addStock(accountId, "005930")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemCount").value(1));
		verify(omniLensMarketQuoteClient, times(1)).getQuote("005930", "USD");
	}

	@Test
	void removeWatchlistItemDeletesAlertTarget() throws Exception {
		String accountId = signUpAndGetAccountId("WatchDelete01");
		when(omniLensMarketQuoteClient.getQuote("000660", "USD"))
				.thenReturn(quote("000660", "SK Hynix", "KOSPI"));
		addStock(accountId, "000660").andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/accounts/{accountId}/watchlist/{stockCode}", accountId, "000660"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemCount").value(0))
				.andExpect(jsonPath("$.data.items", empty()));
	}

	@Test
	void removeUnknownWatchlistItemReturnsCommonError() throws Exception {
		String accountId = signUpAndGetAccountId("WatchMissing01");

		mockMvc.perform(delete("/api/v1/accounts/{accountId}/watchlist/{stockCode}", accountId, "005930"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("WATCHLIST_001"));
	}

	@Test
	void watchlistRejectsInvalidStockCode() throws Exception {
		String accountId = signUpAndGetAccountId("WatchInvalid01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "ABCDEF"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private org.springframework.test.web.servlet.ResultActions addStock(String accountId, String stockCode) throws Exception {
		return mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "stockCode": "%s"
						}
						""".formatted(stockCode)));
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

	private OmniLensMarketQuote quote(String stockCode, String stockNameEn, String market) {
		return new OmniLensMarketQuote(
				stockCode,
				"종목명",
				stockNameEn,
				market,
				new BigDecimal("75000"),
				new BigDecimal("1.25"),
				1000000L,
				new BigDecimal("75000"),
				"KRW",
				new BigDecimal("54.00"),
				"USD",
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}
}
