package com.hana.exchange.market.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.client.OmniLensOrderabilityClient;
import com.hana.exchange.market.client.OmniLensOrderabilityResponse;
import com.hana.exchange.market.domain.MarketQuoteTickMessage;
import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
@AutoConfigureMockMvc
class MarketQuoteStreamControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SimpMessagingTemplate messagingTemplate;

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
	void publishQuoteTickSendsMarketStockAndAccountScopedTopics() throws Exception {
		AuthSession session = fundedAccount("StreamTrader01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "54.00"));
		addWatchlist(session, "005930");
		buy(session, "005930", 1);

		mockMvc.perform(post("/api/v1/market/stream/quotes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(tickPayload("005930", "Samsung Electronics", "KOSPI")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.market").value("KOSPI"))
				.andExpect(jsonPath("$.data.topicCount").value(5))
				.andExpect(jsonPath("$.data.topics[0]").value("/topic/market/quotes"))
				.andExpect(jsonPath("$.data.topics[1]").value("/topic/market/markets/KOSPI"))
				.andExpect(jsonPath("$.data.topics[2]").value("/topic/market/stocks/005930"))
				.andExpect(jsonPath("$.data.topics[3]")
						.value("/topic/accounts/" + session.accountId() + "/market/quotes/watchlist"))
				.andExpect(jsonPath("$.data.topics[4]")
						.value("/topic/accounts/" + session.accountId() + "/market/quotes/portfolio"));

		verify(messagingTemplate).convertAndSend(eq("/topic/market/quotes"), any(MarketQuoteTickMessage.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/market/markets/KOSPI"), any(MarketQuoteTickMessage.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/market/stocks/005930"), any(MarketQuoteTickMessage.class));
		verify(messagingTemplate).convertAndSend(
				eq("/topic/accounts/" + session.accountId() + "/market/quotes/watchlist"),
				any(MarketQuoteTickMessage.class));
		verify(messagingTemplate).convertAndSend(
				eq("/topic/accounts/" + session.accountId() + "/market/quotes/portfolio"),
				any(MarketQuoteTickMessage.class));
	}

	@Test
	void publishQuoteTickRejectsInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/v1/market/stream/quotes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(tickPayload("ABCDEF", "Invalid", "kospi")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private void addWatchlist(AuthSession session, String stockCode) throws Exception {
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "%s"
								}
								""".formatted(stockCode)))
				.andExpect(status().isOk());
	}

	private void buy(AuthSession session, String stockCode, long quantity) throws Exception {
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "%s",
								  "side": "BUY",
								  "quantity": %d,
								  "orderType": "LIMIT",
								  "limitPriceUsd": 54.00
								}
								""".formatted(stockCode, quantity)))
				.andExpect(status().isOk());
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

	private String tickPayload(String stockCode, String stockName, String market) {
		return """
				{
				  "stockCode": "%s",
				  "stockName": "%s",
				  "market": "%s",
				  "currentPriceKrw": 75000,
				  "changeRate": 1.25,
				  "volume": 1000000,
				  "localCurrency": "USD",
				  "localCurrencyPrice": 54.00,
				  "fxRate": 0.00072,
				  "fxRateTime": "2026-06-18T06:00:00Z",
				  "fxRateSource": "HANA_FX_RATE_API",
				  "fxStale": false,
				  "marketDataTime": "2026-06-18T06:00:01Z",
				  "source": "HANA_OMNILENS_API_STREAM"
				}
				""".formatted(stockCode, stockName, market);
	}

	private OmniLensMarketQuote quote(String stockCode, String stockNameEn, String usdPrice) {
		return new OmniLensMarketQuote(
				stockCode,
				"종목명",
				stockNameEn,
				"KOSPI",
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
