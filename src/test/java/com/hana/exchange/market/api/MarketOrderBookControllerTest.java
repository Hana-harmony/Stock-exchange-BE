package com.hana.exchange.market.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.client.OmniLensOrderBookClient;
import com.hana.exchange.market.client.OmniLensOrderBookLevel;
import com.hana.exchange.market.client.OmniLensOrderBookResponse;

@SpringBootTest
@AutoConfigureMockMvc
class MarketOrderBookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensOrderBookClient omniLensOrderBookClient;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void orderBookProxiesHanaOrderBookBoundary() throws Exception {
		when(omniLensOrderBookClient.getOrderBook("005930", "USD"))
				.thenReturn(new OmniLensOrderBookResponse(
						"005930",
						"KOSPI",
						"KRW",
						"USD",
						List.of(new OmniLensOrderBookLevel(
								new BigDecimal("75000"),
								new BigDecimal("54.00"),
								1200,
								12)),
						List.of(new OmniLensOrderBookLevel(
								new BigDecimal("74900"),
								new BigDecimal("53.93"),
								900,
								9)),
						Instant.parse("2026-06-18T06:00:00Z"),
						"HANA_OMNILENS_API"));
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(new OmniLensMarketQuote(
						"005930",
						"삼성전자",
						"Samsung Electronics",
						"KOSPI",
						new BigDecimal("75000"),
						new BigDecimal("1.2"),
						1000,
						new BigDecimal("75000"),
						"KRW",
						new BigDecimal("54.00"),
						"USD",
						0,
						null,
						null,
						null,
						Instant.parse("2026-06-18T06:00:00Z"),
						"KIS_OPEN_API"));

		mockMvc.perform(get("/api/v1/market/stocks/005930/orderbook")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.dataSource").value("HANA_OMNILENS_API"))
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.market").value("KOSPI"))
				.andExpect(jsonPath("$.data.baseCurrency").value("KRW"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.asks[0].priceKrw").value("75000"))
				.andExpect(jsonPath("$.data.asks[0].localCurrencyPrice").value("54"))
				.andExpect(jsonPath("$.data.asks[0].quantity").value(1200))
				.andExpect(jsonPath("$.data.asks[0].orderCount").value(12))
				.andExpect(jsonPath("$.data.bids[0].priceKrw").value("74900"))
				.andExpect(jsonPath("$.data.bids[0].localCurrencyPrice").value("53.93"))
				.andExpect(jsonPath("$.data.marketDataTime").value("2026-06-18T06:00:00Z"));
	}

	@Test
	void orderBookFillsMissingMetadataFromQuote() throws Exception {
		when(omniLensOrderBookClient.getOrderBook("005930", "USD"))
				.thenReturn(new OmniLensOrderBookResponse(
						"005930",
						null,
						null,
						null,
						List.of(new OmniLensOrderBookLevel(
								new BigDecimal("75000"),
								null,
								1200,
								0)),
						List.of(),
						Instant.parse("2026-06-18T06:00:00Z"),
						"KIS_REST_ORDERBOOK"));
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(new OmniLensMarketQuote(
						"005930",
						"삼성전자",
						"Samsung Electronics",
						"KOSPI",
						new BigDecimal("75000"),
						new BigDecimal("1.2"),
						1000,
						new BigDecimal("75000"),
						"KRW",
						new BigDecimal("54.00"),
						"USD",
						0,
						new BigDecimal("0.00072"),
						null,
						null,
						Instant.parse("2026-06-18T06:00:00Z"),
						"KIS_OPEN_API"));

		mockMvc.perform(get("/api/v1/market/stocks/005930/orderbook")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.market").value("KOSPI"))
				.andExpect(jsonPath("$.data.baseCurrency").value("KRW"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.asks[0].localCurrencyPrice").value("54"));
	}

	@Test
	void orderBookReturnsCommonErrorWhenHanaUnavailable() throws Exception {
		when(omniLensOrderBookClient.getOrderBook("005930", "USD"))
				.thenThrow(new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(get("/api/v1/market/stocks/005930/orderbook")
						.param("currency", "USD"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("MARKET_001"));
	}

	@Test
	void orderBookRejectsInvalidStockCodeAndCurrency() throws Exception {
		mockMvc.perform(get("/api/v1/market/stocks/ABCDEF/orderbook")
						.param("currency", "usd"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}
}
