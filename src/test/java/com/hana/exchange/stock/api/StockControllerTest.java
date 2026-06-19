package com.hana.exchange.stock.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.hana.exchange.stock.client.OmniLensStockClient;
import com.hana.exchange.stock.client.OmniLensStockDetailResponse;
import com.hana.exchange.stock.client.OmniLensStockSearchItem;
import com.hana.exchange.stock.client.OmniLensStockSearchResponse;

@SpringBootTest
@AutoConfigureMockMvc
class StockControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensStockClient stockClient;

	@Test
	void searchStocksReturnsEnglishUsdSearchResults() throws Exception {
		when(stockClient.search("samsung", "KOSPI", "USD", 10))
				.thenReturn(new OmniLensStockSearchResponse(
						"samsung",
						"KOSPI",
						"USD",
						List.of(new OmniLensStockSearchItem(
								"005930",
								"삼성전자",
								"Samsung Electronics",
								"KOSPI",
								"Semiconductors",
								"HANA_OMNILENS_API")),
						"HANA_OMNILENS_API"));

		mockMvc.perform(get("/api/v1/stocks/search")
						.param("query", "samsung")
						.param("market", "KOSPI")
						.param("currency", "USD")
						.param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.query").value("samsung"))
				.andExpect(jsonPath("$.data.marketFilter").value("KOSPI"))
				.andExpect(jsonPath("$.data.userLanguage").value("en"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.resultCount").value(1))
				.andExpect(jsonPath("$.data.results[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.results[0].stockName").value("Samsung Electronics"))
				.andExpect(jsonPath("$.data.results[0].dataSource").value("HANA_OMNILENS_API"));
	}

	@Test
	void stockDetailReturnsMarketRiskAndPriceFields() throws Exception {
		when(stockClient.getDetail("005930", "USD"))
				.thenReturn(detail("005930", true, true, "NORMAL", false, true));

		mockMvc.perform(get("/api/v1/stocks/005930")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.stockName").value("Samsung Electronics"))
				.andExpect(jsonPath("$.data.market").value("KOSPI"))
				.andExpect(jsonPath("$.data.baseCurrency").value("KRW"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.currentPriceKrw").value("75000"))
				.andExpect(jsonPath("$.data.localCurrencyPrice").value("54"))
				.andExpect(jsonPath("$.data.foreignOwnershipRate").value("54.5"))
				.andExpect(jsonPath("$.data.viActive").value(true))
				.andExpect(jsonPath("$.data.singlePriceTrading").value(true))
				.andExpect(jsonPath("$.data.priceLimitState").value("NORMAL"))
				.andExpect(jsonPath("$.data.tradingHalted").value(false))
				.andExpect(jsonPath("$.data.orderable").value(true));
	}

	@Test
	void stockProxyReturnsCommonErrorWhenHanaUnavailable() throws Exception {
		when(stockClient.getDetail("005930", "USD"))
				.thenThrow(new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(get("/api/v1/stocks/005930")
						.param("currency", "USD"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("MARKET_001"));
	}

	@Test
	void stockProxyRejectsInvalidQuery() throws Exception {
		mockMvc.perform(get("/api/v1/stocks/search")
						.param("query", "")
						.param("market", "kospi")
						.param("limit", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));

		mockMvc.perform(get("/api/v1/stocks/ABCDEF")
						.param("currency", "usd"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private OmniLensStockDetailResponse detail(
			String stockCode,
			boolean viActive,
			boolean singlePriceTrading,
			String priceLimitState,
			boolean tradingHalted,
			boolean orderable) {
		return new OmniLensStockDetailResponse(
				stockCode,
				"삼성전자",
				"Samsung Electronics",
				"KOSPI",
				"Semiconductors",
				new BigDecimal("75000"),
				new BigDecimal("1.25"),
				1000000L,
				"USD",
				new BigDecimal("54.00"),
				Instant.parse("2026-06-18T06:00:00Z"),
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				viActive,
				singlePriceTrading,
				priceLimitState,
				tradingHalted,
				orderable,
				"HANA_OMNILENS_API");
	}
}
