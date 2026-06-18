package com.hana.exchange.audit.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.client.OmniLensOrderabilityClient;
import com.hana.exchange.market.client.OmniLensOrderabilityResponse;
import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
@AutoConfigureMockMvc
class AuditEventControllerTest {

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
	void auditEventsIncludeTradeNotificationReadAndTaxCaseChanges() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "AuditUser01");
		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": 300.00
								}
								"""))
				.andExpect(status().isOk());
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"))
				.thenReturn(quote("005930", "Samsung Electronics", "70.00"))
				.thenReturn(quote("005930", "Samsung Electronics", "70.00"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930"
								}
								"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "BUY",
								  "quantity": 2
								}
								"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "SELL",
								  "quantity": 1
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(Year.now(ZoneOffset.UTC).getValue())))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("AUDIT-NOTIFY-01", "audit-notify-key-01", "005930")))
				.andExpect(status().isOk());
		MvcResult inbox = mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andReturn();
		String notificationId = JsonPath.read(inbox.getResponse().getContentAsString(),
				"$.data.notifications[0].notificationId");
		mockMvc.perform(post("/api/v1/accounts/{accountId}/notifications/{notificationId}/read",
						session.accountId(), notificationId)
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/audit/events", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accountId").value(session.accountId()))
				.andExpect(jsonPath("$.data.eventCount").value(4))
				.andExpect(jsonPath("$.data.events[0].eventType").value("NOTIFICATION_READ"))
				.andExpect(jsonPath("$.data.events[0].subjectType").value("NOTIFICATION"))
				.andExpect(jsonPath("$.data.events[1].eventType").value("TAX_REFUND_CASE_UPSERTED"))
				.andExpect(jsonPath("$.data.events[1].subjectType").value("TAX_REFUND_CASE"))
				.andExpect(jsonPath("$.data.events[2].eventType").value("TRADE_EXECUTED"))
				.andExpect(jsonPath("$.data.events[2].summary").value(org.hamcrest.Matchers.containsString("SELL 1 005930")))
				.andExpect(jsonPath("$.data.events[3].eventType").value("TRADE_EXECUTED"))
				.andExpect(jsonPath("$.data.events[3].summary").value(org.hamcrest.Matchers.containsString("BUY 2 005930")));
	}

	@Test
	void auditEventsRequireAccountBearerToken() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "AuditAuth01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/audit/events", session.accountId()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void auditEventSummariesAreMaskedBeforeExposure() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "AuditMask01");
		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "taxYear": %d,
								  "treatyCountry": "US",
								  "residenceCertificateFileName": "customer-010-1234-5678.pdf",
								  "reducedTaxApplicationFileName": "token-abcdefghijklmnopqrstuvwxyz123456.pdf",
								  "advancePaymentRequested": true
								}
								""".formatted(Year.now(ZoneOffset.UTC).getValue())))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/audit/events", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.events[0].summary").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("010-1234-5678"))))
				.andExpect(jsonPath("$.data.events[0].subjectId").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("abcdefghijklmnopqrstuvwxyz123456"))));
	}

	private String refundCasePayload(int taxYear) {
		return """
				{
				  "taxYear": %d,
				  "treatyCountry": "US",
				  "residenceCertificateFileName": "residence-certificate.pdf",
				  "reducedTaxApplicationFileName": "reduced-tax-application.pdf",
				  "advancePaymentRequested": true
				}
				""".formatted(taxYear);
	}

	private String eventPayload(String eventId, String idempotencyKey, String stockCode) {
		return """
				{
				  "eventId": "%s",
				  "idempotencyKey": "%s",
				  "sourceType": "DISCLOSURE",
				  "title": "Samsung disclosure update",
				  "summary": "Translated AI summary for local investors",
				  "originalUrl": "https://news.example.com/original",
				  "stockCode": "%s",
				  "relatedStocks": [],
				  "sentiment": "NEUTRAL",
				  "importance": "HIGH",
				  "riskLevel": "LOW",
				  "watchlistTarget": true,
				  "holderTarget": false,
				  "publishedAt": "2026-06-18T06:00:00Z"
				}
				""".formatted(eventId, idempotencyKey, stockCode);
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
