package com.hana.exchange.notification.api;

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
import org.springframework.test.web.servlet.MvcResult;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void alertIngestStoresUnreadNotificationForMatchedWatchlistAccount() throws Exception {
		AuthSession session = watchedAccount("NotifyWatch01", "111111");

		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-01", "notify-key-01", "111111")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.targetCount").value(1));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
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
		AuthSession session = watchedAccount("NotifyIdempotent01", "222222");

		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-02", "notify-key-02", "222222")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-RETRY", "notify-key-02", "222222")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.notifications[0].eventId").value("NOTIFY-EVENT-02"));
	}

	@Test
	void markReadUpdatesNotificationState() throws Exception {
		AuthSession session = watchedAccount("NotifyRead01", "333333");
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("NOTIFY-EVENT-03", "notify-key-03", "333333")))
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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.notificationId").value(notificationId))
				.andExpect(jsonPath("$.data.read").value(true))
				.andExpect(jsonPath("$.data.readAt").exists());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(0));
	}

	@Test
	void markReadRejectsUnknownNotification() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "NotifyMissing01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/notifications/NTF-UNKNOWN00000/read", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("NOTIFICATION_001"));
	}

	@Test
	void registersRefreshesListsAndDisablesDeviceToken() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "NotifyDevice01");

		MvcResult registerResult = mockMvc.perform(post("/api/v1/accounts/{accountId}/notifications/devices", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(devicePayload("IOS", "APNS_PUSH", "ios-device-token-0123456789", "1.0.0", "en_US")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deviceTokenId").value(org.hamcrest.Matchers.startsWith("NTD-")))
				.andExpect(jsonPath("$.data.platform").value("IOS"))
				.andExpect(jsonPath("$.data.provider").value("APNS_PUSH"))
				.andExpect(jsonPath("$.data.tokenHash").exists())
				.andExpect(jsonPath("$.data.maskedToken").value("ios-de...456789"))
				.andExpect(jsonPath("$.data.active").value(true))
				.andExpect(jsonPath("$.data.registeredAt").exists())
				.andExpect(jsonPath("$.data.lastSeenAt").exists())
				.andReturn();
		String deviceTokenId = JsonPath.read(registerResult.getResponse().getContentAsString(),
				"$.data.deviceTokenId");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/notifications/devices", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(devicePayload("IOS", "APNS_PUSH", "ios-device-token-0123456789", "1.0.1", "en_US")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deviceTokenId").value(deviceTokenId))
				.andExpect(jsonPath("$.data.appVersion").value("1.0.1"));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications/devices", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.activeCount").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.devices[0].deviceTokenId").value(deviceTokenId))
				.andExpect(jsonPath("$.data.devices[0].active").value(true));

		mockMvc.perform(delete("/api/v1/accounts/{accountId}/notifications/devices/{deviceTokenId}",
						session.accountId(), deviceTokenId)
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deviceTokenId").value(deviceTokenId))
				.andExpect(jsonPath("$.data.active").value(false))
				.andExpect(jsonPath("$.data.disabledAt").exists());

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications/devices", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.activeCount").value(0))
				.andExpect(jsonPath("$.data.totalCount").value(1));
	}

	@Test
	void disableDeviceRejectsUnknownToken() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "NotifyDeviceMissing01");

		mockMvc.perform(delete("/api/v1/accounts/{accountId}/notifications/devices/NTD-UNKNOWN00000",
						session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("NOTIFICATION_002"));
	}

	private AuthSession watchedAccount(String username, String stockCode) throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, username);
		when(omniLensMarketQuoteClient.getQuote(stockCode, "USD"))
				.thenReturn(quote(stockCode));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/watchlist", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "%s"
								}
				""".formatted(stockCode)))
				.andExpect(status().isOk());
		return session;
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

	private String devicePayload(
			String platform,
			String provider,
			String deviceToken,
			String appVersion,
			String locale) {
		return """
				{
				  "platform": "%s",
				  "provider": "%s",
				  "deviceToken": "%s",
				  "appVersion": "%s",
				  "locale": "%s"
				}
				""".formatted(platform, provider, deviceToken, appVersion, locale);
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
