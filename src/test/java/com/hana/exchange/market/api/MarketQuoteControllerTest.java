package com.hana.exchange.market.api;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;

@SpringBootTest
@AutoConfigureMockMvc
class MarketQuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void quotesProxyConfiguredUniverseAsRestSnapshot() throws Exception {
		when(omniLensMarketQuoteClient.getAllQuotes("USD"))
				.thenReturn(List.of(
						quote("005930", "삼성전자", "Samsung Electronics", "KOSPI", "75000", "54.00"),
						quote("000660", "SK하이닉스", "SK hynix", "KOSPI", "180000", "129.60")));

		mockMvc.perform(get("/api/v1/market/quotes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.code").value("COMMON_000"))
				.andExpect(jsonPath("$.data.dataSource").value("HANA_OMNILENS_API"))
				.andExpect(jsonPath("$.data.marketCoverage").value("ALL_KOREAN_STOCKS"))
				.andExpect(jsonPath("$.data.userLanguage").value("en"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.tradingMode").value("EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING"))
				.andExpect(jsonPath("$.data.transport.snapshot").value("REST"))
				.andExpect(jsonPath("$.data.transport.realtime").value("WebSocket"))
				.andExpect(jsonPath("$.data.quoteCount").value(2))
				.andExpect(jsonPath("$.data.quotes[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.quotes[0].stockName").value("Samsung Electronics (삼성전자)"))
				.andExpect(jsonPath("$.data.quotes[0].market").value("KOSPI"))
				.andExpect(jsonPath("$.data.quotes[0].currentPriceKrw").value("75000"))
				.andExpect(jsonPath("$.data.quotes[0].localCurrencyPrice").value("54"));
	}

	@Test
	void quotesAcceptRequestedStockCodesAndMarketFilter() throws Exception {
		when(omniLensMarketQuoteClient.getQuotes(List.of("005930", "091990"), "USD"))
				.thenReturn(List.of(
						quote("005930", "삼성전자", "Samsung Electronics", "KOSPI", "75000", "54.00"),
						quote("091990", "셀트리온헬스케어", "Celltrion Healthcare", "KOSDAQ", "66000", "47.52")));

		mockMvc.perform(get("/api/v1/market/quotes")
						.param("stockCodes", "005930", "091990")
						.param("market", "KOSDAQ")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.marketCoverage").value("REQUESTED_STOCK_CODES"))
				.andExpect(jsonPath("$.data.marketFilter").value("KOSDAQ"))
				.andExpect(jsonPath("$.data.quoteCount").value(1))
				.andExpect(jsonPath("$.data.quotes[0].stockCode").value("091990"))
				.andExpect(jsonPath("$.data.quotes[0].market").value("KOSDAQ"));
	}

	@Test
	void singleQuoteProxiesOmniLensRestSnapshot() throws Exception {
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(new OmniLensMarketQuote(
						"005930",
						"삼성전자",
						"Samsung Electronics",
						"KOSPI",
						new BigDecimal("75000"),
						new BigDecimal("1.25"),
						1000000L,
						new BigDecimal("75000"),
						"KRW",
						new BigDecimal("54.00"),
						"USD",
						new BigDecimal("0.00073"),
						Instant.parse("2026-06-18T05:59:30Z"),
						"HANA_FX_RATE_API",
						true,
						50000000L,
						new BigDecimal("54.5"),
						new BigDecimal("72.3"),
						LocalDate.parse("2026-06-18"),
						Instant.parse("2026-06-18T06:00:00Z"),
						"HANA_OMNILENS_API"));

		mockMvc.perform(get("/api/v1/market/quotes/005930")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.dataSource").value("HANA_OMNILENS_API"))
				.andExpect(jsonPath("$.data.marketCoverage").value("005930"))
				.andExpect(jsonPath("$.data.displayCurrency").value("USD"))
				.andExpect(jsonPath("$.data.quotes[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.quotes[0].stockName").value("Samsung Electronics (삼성전자)"))
				.andExpect(jsonPath("$.data.quotes[0].market").value("KOSPI"))
				.andExpect(jsonPath("$.data.quotes[0].currentPriceKrw").value("75000"))
				.andExpect(jsonPath("$.data.quotes[0].changeRate").value("1.25"))
				.andExpect(jsonPath("$.data.quotes[0].volume").value(1000000))
				.andExpect(jsonPath("$.data.quotes[0].localCurrency").value("USD"))
				.andExpect(jsonPath("$.data.quotes[0].localCurrencyPrice").value("54"))
				.andExpect(jsonPath("$.data.quotes[0].fxRate").value("0.00073"))
				.andExpect(jsonPath("$.data.quotes[0].fxRateTime").value("2026-06-18T05:59:30Z"))
				.andExpect(jsonPath("$.data.quotes[0].fxRateSource").value("HANA_FX_RATE_API"))
				.andExpect(jsonPath("$.data.quotes[0].fxStale").value(true));
	}

	@Test
	void singleQuoteReturnsCommonErrorWhenOmniLensUnavailable() throws Exception {
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenThrow(new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(get("/api/v1/market/quotes/005930")
						.param("currency", "USD"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("MARKET_001"));
	}

	@Test
	void singleQuoteRejectsInvalidStockCodeAndCurrency() throws Exception {
		mockMvc.perform(get("/api/v1/market/quotes/ABCDEF")
						.param("currency", "usd"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	@Test
	void quotesRejectInvalidStockCodesAndMarket() throws Exception {
		mockMvc.perform(get("/api/v1/market/quotes")
						.param("stockCodes", "ABCDEF")
						.param("market", "kospi"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private OmniLensMarketQuote quote(
			String stockCode,
			String stockName,
			String stockNameEn,
			String market,
			String currentPriceKrw,
			String localCurrencyPrice) {
		return new OmniLensMarketQuote(
				stockCode,
				stockName,
				stockNameEn,
				market,
				new BigDecimal(currentPriceKrw),
				new BigDecimal("1.25"),
				1000000L,
				new BigDecimal(currentPriceKrw),
				"KRW",
				new BigDecimal(localCurrencyPrice),
				"USD",
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}
}
