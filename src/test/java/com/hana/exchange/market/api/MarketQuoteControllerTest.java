package com.hana.exchange.market.api;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void quotesExposePlannedRealtimeContract() throws Exception {
		mockMvc.perform(get("/api/v1/market/quotes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.code").value("COMMON_000"))
				.andExpect(jsonPath("$.data.dataSource").value("HANA_OMNILENS_API_PLANNED"))
				.andExpect(jsonPath("$.data.marketCoverage").value("ALL_KOREAN_LISTED_STOCKS"))
				.andExpect(jsonPath("$.data.userLanguage").value("en"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.tradingMode").value("EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING"))
				.andExpect(jsonPath("$.data.transport.snapshot").value("REST"))
				.andExpect(jsonPath("$.data.transport.realtime").value("WebSocket"))
				.andExpect(jsonPath("$.data.quotes", empty()));
	}
}
