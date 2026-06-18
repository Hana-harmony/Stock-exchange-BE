package com.hana.exchange.market.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketHistoryClient;
import com.hana.exchange.market.client.OmniLensMarketHistoryPoint;
import com.hana.exchange.market.client.OmniLensMarketHistoryResponse;

@SpringBootTest
@AutoConfigureMockMvc
class MarketChartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketHistoryClient historyClient;

	@Test
	void chartProxiesOmniLensKrxHistoryForFlutterChart() throws Exception {
		LocalDate from = LocalDate.parse("2026-06-17");
		LocalDate to = LocalDate.parse("2026-06-18");
		when(historyClient.getHistory("005930", from, to, "1d", "USD"))
				.thenReturn(new OmniLensMarketHistoryResponse(
						"005930",
						"1d",
						"KRW",
						"USD",
						List.of(
								point("2026-06-17", "74000", "75200", "73800", "75000", "53.28", "54.14", "53.14", "54.00"),
								point("2026-06-18", "75000", "76100", "74700", "75800", "54.00", "54.79", "53.78", "54.58")),
						"HANA_OMNILENS_KRX_HISTORY"));

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-17")
						.param("to", "2026-06-18")
						.param("interval", "1d")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.dataSource").value("HANA_OMNILENS_KRX_HISTORY"))
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.interval").value("1d"))
				.andExpect(jsonPath("$.data.baseCurrency").value("KRW"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.pointCount").value(2))
				.andExpect(jsonPath("$.data.points[0].tradeDate").value("2026-06-17"))
				.andExpect(jsonPath("$.data.points[0].openPriceKrw").value("74000"))
				.andExpect(jsonPath("$.data.points[0].closeLocalCurrencyPrice").value("54"))
				.andExpect(jsonPath("$.data.points[0].volume").value(1000000))
				.andExpect(jsonPath("$.data.points[0].adjusted").value(true));
	}

	@Test
	void chartReturnsCommonErrorWhenOmniLensHistoryUnavailable() throws Exception {
		LocalDate from = LocalDate.parse("2026-06-17");
		LocalDate to = LocalDate.parse("2026-06-18");
		when(historyClient.getHistory("005930", from, to, "1d", "USD"))
				.thenThrow(new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-17")
						.param("to", "2026-06-18"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("MARKET_001"));
	}

	@Test
	void chartRejectsInvalidRangeAndParameters() throws Exception {
		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-18")
						.param("to", "2026-06-17"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_001"));

		mockMvc.perform(get("/api/v1/market/stocks/ABCDEF/chart")
						.param("from", "2026-06-17")
						.param("to", "2026-06-18")
						.param("interval", "5m")
						.param("currency", "usd"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private OmniLensMarketHistoryPoint point(
			String tradeDate,
			String openKrw,
			String highKrw,
			String lowKrw,
			String closeKrw,
			String openLocal,
			String highLocal,
			String lowLocal,
			String closeLocal) {
		return new OmniLensMarketHistoryPoint(
				LocalDate.parse(tradeDate),
				new BigDecimal(openKrw),
				new BigDecimal(highKrw),
				new BigDecimal(lowKrw),
				new BigDecimal(closeKrw),
				new BigDecimal(openLocal),
				new BigDecimal(highLocal),
				new BigDecimal(lowLocal),
				new BigDecimal(closeLocal),
				1000000L,
				new BigDecimal("75000000000"),
				true);
	}
}
