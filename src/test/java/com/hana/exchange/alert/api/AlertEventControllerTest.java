package com.hana.exchange.alert.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
class AlertEventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@MockitoBean
	private OmniLensOrderabilityClient omniLensOrderabilityClient;

	@BeforeEach
	void allowMockOrdersByDefault() {
		when(omniLensOrderabilityClient.checkOrderability(anyString(), any(TradeSide.class), anyLong()))
				.thenAnswer(invocation -> orderability(invocation.getArgument(0)));
	}

	@Test
	void ingestEventMatchesWatchlistAndHoldingTargets() throws Exception {
		AuthSession watchSession = AuthTestSupport.signUpAndLogin(mockMvc, "AlertWatch01");
		AuthSession holderSession = fundedAccount("AlertHolder01", "300.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "54.00"))
				.thenReturn(quote("005930", "Samsung Electronics", "54.00"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", watchSession.accountId())
						.header(HttpHeaders.AUTHORIZATION, watchSession.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930"
								}
								"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", holderSession.accountId())
						.header(HttpHeaders.AUTHORIZATION, holderSession.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "BUY",
								  "quantity": 2
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("ALERT-WATCH-HOLDER-01", "alert-key-01", "005930", true, true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.eventId").value("ALERT-WATCH-HOLDER-01"))
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.targetCount").value(2))
				.andExpect(jsonPath("$.data.targets[0].matchedStockCodes[0]").value("005930"))
				.andExpect(jsonPath("$.data.targets[1].matchedStockCodes[0]").value("005930"));
	}

	@Test
	void ingestEventIsIdempotentByIdempotencyKey() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "AlertIdempotent01");
		when(omniLensMarketQuoteClient.getQuote("000660", "USD"))
				.thenReturn(quote("000660", "SK Hynix", "120.00"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "000660"
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("ALERT-IDEMPOTENT-01", "alert-key-02", "000660", true, false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.targetCount").value(1));
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("ALERT-IDEMPOTENT-RETRY", "alert-key-02", "000660", true, false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.eventId").value("ALERT-IDEMPOTENT-01"))
				.andExpect(jsonPath("$.data.targetCount").value(1));
		verify(omniLensMarketQuoteClient, times(1)).getQuote("000660", "USD");
	}

	@Test
	void getTargetsReturnsStoredMatchResult() throws Exception {
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("ALERT-GET-01", "alert-key-03", "035420", true, true)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/alerts/events/ALERT-GET-01/targets"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.eventId").value("ALERT-GET-01"))
				.andExpect(jsonPath("$.data.targetCount").value(0));
	}

	@Test
	void getTargetsRejectsUnknownEvent() throws Exception {
		mockMvc.perform(get("/api/v1/alerts/events/UNKNOWN-EVENT/targets"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("ALERT_001"));
	}

	@Test
	void ingestRejectsInvalidStockCode() throws Exception {
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("ALERT-INVALID-01", "alert-key-04", "ABCDEF", true, false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
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

	private String eventPayload(
			String eventId,
			String idempotencyKey,
			String stockCode,
			boolean watchlistTarget,
			boolean holderTarget) {
		return """
				{
				  "eventId": "%s",
				  "idempotencyKey": "%s",
				  "sourceType": "NEWS",
				  "title": "Samsung supply chain update",
				  "summary": "Translated AI summary for local investors",
				  "originalUrl": "https://news.example.com/original",
				  "stockCode": "%s",
				  "relatedStocks": [],
				  "sentiment": "POSITIVE",
				  "importance": "HIGH",
				  "riskLevel": "MEDIUM",
				  "watchlistTarget": %s,
				  "holderTarget": %s,
				  "publishedAt": "2026-06-18T06:00:00Z"
				}
				""".formatted(eventId, idempotencyKey, stockCode, watchlistTarget, holderTarget);
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
