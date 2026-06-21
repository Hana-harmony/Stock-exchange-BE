package com.hana.exchange.market.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import com.hana.exchange.market.client.OmniLensOrderabilityClient;
import com.hana.exchange.market.client.OmniLensOrderabilityResponse;
import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
@AutoConfigureMockMvc
class AccountMarketQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@MockitoBean
	private OmniLensOrderabilityClient omniLensOrderabilityClient;

	@MockitoBean
	private Clock clock;

	@BeforeEach
	void prepareTradingSession() {
		when(clock.instant()).thenReturn(Instant.parse("2026-06-18T01:00:00Z"));
		when(omniLensOrderabilityClient.checkOrderability(anyString(), any(TradeSide.class), anyLong()))
				.thenAnswer(invocation -> orderability(invocation.getArgument(0)));
	}

	@Test
	void watchlistQuotesReturnOnlyAccountWatchlistStocks() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "QuoteWatch01");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "KOSPI", "54.00"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930"
								}
								"""))
				.andExpect(status().isOk());

		when(omniLensMarketQuoteClient.getQuotes(List.of("005930"), "USD"))
				.thenReturn(List.of(quote("005930", "Samsung Electronics", "KOSPI", "55.00")));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/market/quotes/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
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
		AuthSession session = fundedAccount("QuoteHolding01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("000660", "USD"))
				.thenReturn(quote("000660", "SK hynix", "KOSPI", "50.00"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "000660",
								  "side": "BUY",
								  "quantity": 2,
								  "orderType": "LIMIT",
								  "limitPriceUsd": 50.00
								}
								"""))
				.andExpect(status().isOk());

		when(omniLensMarketQuoteClient.getQuotes(List.of("000660"), "USD"))
				.thenReturn(List.of(quote("000660", "SK hynix", "KOSPI", "60.00")));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/market/quotes/portfolio", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
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
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "QuoteEmpty01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/market/quotes/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.marketCoverage").value("WATCHLIST_STOCKS"))
				.andExpect(jsonPath("$.data.quoteCount").value(0));
	}

	@Test
	void accountQuoteViewsRejectMissingBearerToken() throws Exception {
		mockMvc.perform(get("/api/v1/accounts/ACC-UNKNOWN00000/market/quotes/portfolio"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_003"));
	}

	private AuthSession fundedAccount(String username, String amountUsd) throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, username);
		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": %s
								}
								""".formatted(amountUsd)))
				.andExpect(status().isOk());
		return session;
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

	private OmniLensOrderabilityResponse orderability(String stockCode) {
		return new OmniLensOrderabilityResponse(
				stockCode,
				"KOSPI",
				true,
				null,
				false,
				false,
				"NORMAL",
				false,
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}
}
