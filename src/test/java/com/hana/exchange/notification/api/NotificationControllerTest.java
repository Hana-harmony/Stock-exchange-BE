package com.hana.exchange.notification.api;

import static org.mockito.Mockito.when;
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
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void alertIngestStoresUnreadNotificationForMatchedWatchlistAccount() throws Exception {
		String accountId = watchedAccount("NotifyWatch01", "111111");

		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-01", "notify-key-01", "111111")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.targetCount").value(1));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.notifications[0].eventId").value("NOTIFY-EVENT-01"))
				.andExpect(jsonPath("$.data.notifications[0].title").value("Samsung disclosure update"))
				.andExpect(jsonPath("$.data.notifications[0].originalUrl").value("https://news.example.com/original"))
				.andExpect(jsonPath("$.data.notifications[0].matchedStockCodes[0]").value("111111"))
				.andExpect(jsonPath("$.data.notifications[0].matchReasons[0]").value("WATCHLIST"))
				.andExpect(jsonPath("$.data.notifications[0].deliveryStatus").value("DELIVERED"))
				.andExpect(jsonPath("$.data.notifications[0].deliveryProvider").value("LOCAL_NOOP_PUSH"))
				.andExpect(jsonPath("$.data.notifications[0].deliveryAttemptCount").value(1))
				.andExpect(jsonPath("$.data.notifications[0].deliveredAt").exists())
				.andExpect(jsonPath("$.data.notifications[0].read").value(false));
	}

	@Test
	void duplicateAlertEventDoesNotDuplicateNotification() throws Exception {
		String accountId = watchedAccount("NotifyIdempotent01", "222222");

		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-02", "notify-key-02", "222222")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-RETRY", "notify-key-02", "222222")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.notifications[0].eventId").value("NOTIFY-EVENT-02"));
	}

	@Test
	void markReadUpdatesNotificationState() throws Exception {
		String accountId = watchedAccount("NotifyRead01", "333333");
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-03", "notify-key-03", "333333")))
				.andExpect(status().isOk());
		MvcResult inbox = mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", accountId))
				.andExpect(status().isOk())
				.andReturn();
		String notificationId = JsonPath.read(inbox.getResponse().getContentAsString(),
				"$.data.notifications[0].notificationId");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/notifications/{notificationId}/read", accountId, notificationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.notificationId").value(notificationId))
				.andExpect(jsonPath("$.data.read").value(true))
				.andExpect(jsonPath("$.data.readAt").exists());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(0));
	}

	@Test
	void markReadRejectsUnknownNotification() throws Exception {
		String accountId = signUpAndGetAccountId("NotifyMissing01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/notifications/NTF-UNKNOWN00000/read", accountId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("NOTIFICATION_001"));
	}

	private String watchedAccount(String username, String stockCode) throws Exception {
		String accountId = signUpAndGetAccountId(username);
		when(omniLensMarketQuoteClient.getQuote(stockCode, "USD"))
				.thenReturn(quote(stockCode));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "%s"
								}
								""".formatted(stockCode)))
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

	private OmniLensMarketQuote quote(String stockCode) {
		return new OmniLensMarketQuote(
				stockCode,
				"종목명",
				"Stock " + stockCode,
				"KOSPI",
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
