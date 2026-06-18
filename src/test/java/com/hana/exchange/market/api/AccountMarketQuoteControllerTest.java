package com.hana.exchange.market.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
class AccountMarketQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void watchlistQuotesReturnOnlyAccountWatchlistStocks() throws Exception {
		String accountId = signUpAndGetAccountId("QuoteWatch01");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "KOSPI", "54.00"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930"
								}
								"""))
				.andExpect(status().isOk());

		when(omniLensMarketQuoteClient.getQuotes(List.of("005930"), "USD"))
				.thenReturn(List.of(quote("005930", "Samsung Electronics", "KOSPI", "55.00")));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/market/quotes/watchlist", accountId)
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.marketCoverage").value("WATCHLIST_STOCKS"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.quoteCount").value(1))
				.andExpect(jsonPath("$.data.quotes[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.quotes[0].localCurrencyPrice").value("55"));
	}

	@Test
	void portfolioQuotesReturnOnlyCurrentHoldings() throws Exception {
		String accountId = fundedAccount("QuoteHolding01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("000660", "USD"))
				.thenReturn(quote("000660", "SK hynix", "KOSPI", "50.00"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "000660",
								  "side": "BUY",
								  "quantity": 2
								}
								"""))
				.andExpect(status().isOk());

		when(omniLensMarketQuoteClient.getQuotes(List.of("000660"), "USD"))
				.thenReturn(List.of(quote("000660", "SK hynix", "KOSPI", "60.00")));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/market/quotes/portfolio", accountId)
						.param("market", "KOSPI")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.marketCoverage").value("PORTFOLIO_HOLDINGS"))
				.andExpect(jsonPath("$.data.marketFilter").value("KOSPI"))
				.andExpect(jsonPath("$.data.quoteCount").value(1))
				.andExpect(jsonPath("$.data.quotes[0].stockCode").value("000660"))
				.andExpect(jsonPath("$.data.quotes[0].localCurrencyPrice").value("60"));
	}

	@Test
	void emptyWatchlistDoesNotFallBackToDefaultUniverse() throws Exception {
		String accountId = signUpAndGetAccountId("QuoteEmpty01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/market/quotes/watchlist", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.marketCoverage").value("WATCHLIST_STOCKS"))
				.andExpect(jsonPath("$.data.quoteCount").value(0));
	}

	@Test
	void accountQuoteViewsRejectUnknownAccount() throws Exception {
		mockMvc.perform(get("/api/v1/accounts/ACC-UNKNOWN00000/market/quotes/portfolio"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("ACCOUNT_001"));
	}

	private String fundedAccount(String username, String amountUsd) throws Exception {
		String accountId = signUpAndGetAccountId(username);
		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": %s
								}
								""".formatted(amountUsd)))
				.andExpect(status().isOk());
		return accountId;
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

	private OmniLensMarketQuote quote(String stockCode, String stockNameEn, String market, String usdPrice) {
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
				new BigDecimal(usdPrice),
				"USD",
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}
}
