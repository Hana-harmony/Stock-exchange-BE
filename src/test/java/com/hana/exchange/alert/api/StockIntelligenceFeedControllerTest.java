package com.hana.exchange.alert.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StockIntelligenceFeedControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void stockFeedReturnsStoredAnalyzedEventsWithOriginalLinks() throws Exception {
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("FEED-EVENT-01", "feed-key-01", "444444", "000660")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/stocks/444444/intelligence"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.stockCode").value("444444"))
				.andExpect(jsonPath("$.data.dataSource").value("HANA_OMNILENS_AI_ANALYZED_EVENT"))
				.andExpect(jsonPath("$.data.itemCount").value(1))
				.andExpect(jsonPath("$.data.items[0].eventId").value("FEED-EVENT-01"))
				.andExpect(jsonPath("$.data.items[0].sourceType").value("NEWS"))
				.andExpect(jsonPath("$.data.items[0].summary").value("Translated AI summary for feed"))
				.andExpect(jsonPath("$.data.items[0].originalUrl").value("https://news.example.com/feed-original"))
				.andExpect(jsonPath("$.data.items[0].sentiment").value("POSITIVE"))
				.andExpect(jsonPath("$.data.items[0].importance").value("HIGH"))
				.andExpect(jsonPath("$.data.items[0].riskLevel").value("MEDIUM"));
	}

	@Test
	void stockFeedMatchesRelatedStocks() throws Exception {
		mockMvc.perform(post("/api/v1/alerts/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventPayload("FEED-EVENT-02", "feed-key-02", "035420", "555555")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/stocks/555555/intelligence"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].eventId").value("FEED-EVENT-02"))
				.andExpect(jsonPath("$.data.items[0].primaryStockCode").value("035420"))
				.andExpect(jsonPath("$.data.items[0].relatedStocks[0]").value("555555"));
	}

	@Test
	void stockFeedRejectsInvalidStockCode() throws Exception {
		mockMvc.perform(get("/api/v1/stocks/ABCDEF/intelligence"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private String eventPayload(String eventId, String idempotencyKey, String stockCode, String relatedStockCode) {
		return """
				{
				  "eventId": "%s",
				  "idempotencyKey": "%s",
				  "sourceType": "NEWS",
				  "title": "Market intelligence feed update",
				  "summary": "Translated AI summary for feed",
				  "originalUrl": "https://news.example.com/feed-original",
				  "stockCode": "%s",
				  "relatedStocks": ["%s"],
				  "sentiment": "POSITIVE",
				  "importance": "HIGH",
				  "riskLevel": "MEDIUM",
				  "watchlistTarget": true,
				  "holderTarget": true,
				  "publishedAt": "2026-06-18T06:00:00Z"
				}
				""".formatted(eventId, idempotencyKey, stockCode, relatedStockCode);
	}
}
