package com.hana.exchange.marketnews.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.marketnews.client.OmniLensMarketNewsClient;
import com.hana.exchange.marketnews.client.OmniLensMarketNewsEvent;
import com.hana.exchange.marketnews.client.OmniLensMarketNewsListResponse;

@SpringBootTest
@AutoConfigureMockMvc
class MarketNewsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketNewsClient marketNewsClient;

	@Test
	void listMarketNewsReturnsOmniLensMarketWideNews() throws Exception {
		when(marketNewsClient.getLatest(5)).thenReturn(new OmniLensMarketNewsListResponse(
				1,
				List.of(marketNews())));

		mockMvc.perform(get("/api/v1/market/news?limit=5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.newsCount").value(1))
				.andExpect(jsonPath("$.data.news[0].newsId").value("MKT-NEWS-001"))
				.andExpect(jsonPath("$.data.news[0].title").value("Ants lift chip bellwethers"))
				.andExpect(jsonPath("$.data.news[0].originalContent").value("Ants net bought the KOSPI bellwether."));
	}

	@Test
	void marketNewsCorsPreflightAllowsLocalFrontendOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/market/news?limit=5")
						.header("Origin", "http://127.0.0.1:15100")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:15100"))
				.andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
	}

	@Test
	void getMarketNewsReturnsDetailPayload() throws Exception {
		when(marketNewsClient.getByNewsId("MKT-NEWS-001")).thenReturn(marketNews());

		mockMvc.perform(get("/api/v1/market/news/MKT-NEWS-001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.newsId").value("MKT-NEWS-001"))
				.andExpect(jsonPath("$.data.contentAvailability").value("FULL_TEXT"));
	}

	@Test
	void listMarketNewsValidatesLimit() throws Exception {
		mockMvc.perform(get("/api/v1/market/news?limit=1000"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	@Test
	void listMarketNewsReturnsCommonErrorWhenOmniLensUnavailable() throws Exception {
		when(marketNewsClient.getLatest(10))
				.thenThrow(new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(get("/api/v1/market/news"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("MARKET_001"));
	}

	private OmniLensMarketNewsEvent marketNews() {
		return new OmniLensMarketNewsEvent(
				"MKT-NEWS-001",
				"한국 증시",
				"Ants lift chip bellwethers",
				"Ants bought semiconductor bellwethers.",
				"Ants net bought the KOSPI bellwether.",
				List.of("https://img.example.com/1.jpg"),
				"FULL_TEXT",
				"https://news.example.com/1",
				"https://news.example.com/1",
				"LINK_ONLY",
				"market-news-1",
				Instant.parse("2026-07-02T00:00:00Z"),
				Instant.parse("2026-07-02T00:01:00Z"));
	}
}
