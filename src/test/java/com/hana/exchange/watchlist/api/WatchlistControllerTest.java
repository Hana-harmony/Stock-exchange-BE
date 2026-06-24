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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;

@SpringBootTest
@AutoConfigureMockMvc
class WatchlistControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void getWatchlistReturnsEmptyItemsForNewAccount() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "WatchEmpty01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accountId").value(session.accountId()))
				.andExpect(jsonPath("$.data.itemCount").value(0))
				.andExpect(jsonPath("$.data.targetingMode").value("WATCHLIST_ALERT_TARGET"))
				.andExpect(jsonPath("$.data.items", empty()));
	}

	@Test
	void addWatchlistItemUsesOmniLensMetadataAndIsIdempotent() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "WatchAdd01");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "KOSPI"));

		addStock(session, "005930")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemCount").value(1))
				.andExpect(jsonPath("$.data.items[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.items[0].stockName").value("Samsung Electronics (종목명)"))
				.andExpect(jsonPath("$.data.items[0].market").value("KOSPI"))
				.andExpect(jsonPath("$.data.items[0].targetingMode").value("WATCHLIST_ALERT_TARGET"));

		addStock(session, "005930")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemCount").value(1));
		verify(omniLensMarketQuoteClient, times(1)).getQuote("005930", "USD");
	}

	@Test
	void removeWatchlistItemDeletesAlertTarget() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "WatchDelete01");
		when(omniLensMarketQuoteClient.getQuote("000660", "USD"))
				.thenReturn(quote("000660", "SK Hynix", "KOSPI"));
		addStock(session, "000660").andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/accounts/{accountId}/watchlist/{stockCode}", session.accountId(), "000660")
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.itemCount").value(0))
				.andExpect(jsonPath("$.data.items", empty()));
	}

	@Test
	void removeUnknownWatchlistItemReturnsCommonError() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "WatchMissing01");

		mockMvc.perform(delete("/api/v1/accounts/{accountId}/watchlist/{stockCode}", session.accountId(), "005930")
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("WATCHLIST_001"));
	}

	@Test
	void watchlistRejectsInvalidStockCode() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "WatchInvalid01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
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

	private org.springframework.test.web.servlet.ResultActions addStock(AuthSession session, String stockCode) throws Exception {
		return mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
				.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "stockCode": "%s"
						}
						""".formatted(stockCode)));
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
