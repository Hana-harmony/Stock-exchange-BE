package com.hana.exchange.market.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.hana.exchange.market.client.OmniLensMarketIntradayClient;
import com.hana.exchange.market.client.OmniLensMarketIntradayPrice;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;

@SpringBootTest
@AutoConfigureMockMvc
class MarketChartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketHistoryClient historyClient;

	@MockitoBean
	private OmniLensMarketIntradayClient intradayClient;

	@MockitoBean
	private OmniLensMarketQuoteClient quoteClient;

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
								point("2026-06-17", "74000", "75200", "73800", "75000"),
								point("2026-06-18", "75000", "76100", "74700", "75800")),
						"KRX_OPEN_API_DAILY_TRADE"));
		when(quoteClient.getQuote("005930", "USD")).thenReturn(quote());

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-17")
						.param("to", "2026-06-18")
						.param("interval", "1d")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.dataSource").value("KRX_OPEN_API_DAILY_TRADE"))
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
	void chartAggregatesWeeklyOhlcvForFlutterChart() throws Exception {
		LocalDate from = LocalDate.parse("2026-06-15");
		LocalDate to = LocalDate.parse("2026-06-16");
		when(historyClient.getHistory("005930", from, to, "1w", "USD"))
				.thenReturn(new OmniLensMarketHistoryResponse(
						"005930",
						"1w",
						"KRW",
						"USD",
						List.of(
								point("2026-06-15", "74000", "75200", "73800", "75000"),
								point("2026-06-16", "75000", "76100", "74700", "75800")),
						"KRX_OPEN_API_DAILY_TRADE"));
		when(quoteClient.getQuote("005930", "USD")).thenReturn(quote());

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-15")
						.param("to", "2026-06-16")
						.param("interval", "1w")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.interval").value("1w"))
				.andExpect(jsonPath("$.data.pointCount").value(1))
				.andExpect(jsonPath("$.data.points[0].tradeDate").value("2026-06-15"))
				.andExpect(jsonPath("$.data.points[0].openPriceKrw").value("74000"))
				.andExpect(jsonPath("$.data.points[0].highPriceKrw").value("76100"))
				.andExpect(jsonPath("$.data.points[0].lowPriceKrw").value("73800"))
				.andExpect(jsonPath("$.data.points[0].closePriceKrw").value("75800"))
				.andExpect(jsonPath("$.data.points[0].closeLocalCurrencyPrice").value("54.576"))
				.andExpect(jsonPath("$.data.points[0].volume").value(2000000))
				.andExpect(jsonPath("$.data.points[0].tradingValueKrw").value("150000000000"));
	}

	@Test
	void chartUsesOmniLensIntradayPricesForOneDayMinuteChart() throws Exception {
		LocalDate date = LocalDate.parse("2026-06-24");
		when(quoteClient.getQuote("005930", "USD")).thenReturn(quote());
		when(intradayClient.getIntraday("005930", date, 390)).thenReturn(List.of(
				intraday("2026-06-24T09:01:00", "75000", "75200", "75000", "75200", 1_001_000L),
				intraday("2026-06-24T09:02:00", "75200", "75300", "75100", "75100", 1_002_000L)));

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
							.param("from", "2026-06-24")
							.param("to", "2026-06-24")
							.param("interval", "1m")
							.param("currency", "USD"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.dataSource").value("KIS_TIME_ITEM_CHART_PRICE"))
					.andExpect(jsonPath("$.data.interval").value("1m"))
					.andExpect(jsonPath("$.data.pointCount").value(2))
					.andExpect(jsonPath("$.data.points[0].tradeDate").value("2026-06-24T09:01"))
					.andExpect(jsonPath("$.data.points[0].openPriceKrw").value("75000"))
					.andExpect(jsonPath("$.data.points[0].highPriceKrw").value("75200"))
					.andExpect(jsonPath("$.data.points[0].lowPriceKrw").value("75000"))
					.andExpect(jsonPath("$.data.points[0].closePriceKrw").value("75200"))
					.andExpect(jsonPath("$.data.points[0].closeLocalCurrencyPrice").value("54.144"))
					.andExpect(jsonPath("$.data.points[0].volume").value(1001000))
					.andExpect(jsonPath("$.data.points[1].tradeDate").value("2026-06-24T09:02"));
	}

	@Test
	void chartDoesNotFallbackToSingleDailyCandleWhenMinutePricesAreOutsideRegularSession() throws Exception {
		LocalDate date = LocalDate.parse("2026-07-01");
		when(quoteClient.getQuote("005930", "USD")).thenReturn(quote());
		when(intradayClient.getIntraday("005930", date, 390)).thenReturn(List.of(
				intraday("2026-07-01T23:59:00", "60000", "60000", "60000", "60000", 1_000L)));

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-07-01")
						.param("to", "2026-07-01")
						.param("interval", "1m")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.dataSource").value("KIS_TIME_ITEM_CHART_PRICE"))
				.andExpect(jsonPath("$.data.interval").value("1m"))
				.andExpect(jsonPath("$.data.from").value("2026-07-01"))
				.andExpect(jsonPath("$.data.to").value("2026-07-01"))
				.andExpect(jsonPath("$.data.pointCount").value(0));
	}

	@Test
	void chartAggregatesMultiDayIntradayPricesForOneWeekChart() throws Exception {
		LocalDate from = LocalDate.parse("2026-06-24");
		LocalDate to = LocalDate.parse("2026-06-25");
		when(quoteClient.getQuote("005930", "USD")).thenReturn(quote());
		when(intradayClient.getIntraday("005930", LocalDate.parse("2026-06-24"), 390, true)).thenReturn(List.of(
				intraday("2026-06-24T09:01:00", "75000", "75200", "75000", "75100", 1_000L),
				intraday("2026-06-24T09:29:00", "75100", "75300", "75050", "75200", 2_000L),
				intraday("2026-06-24T09:31:00", "75200", "75400", "75100", "75300", 3_000L)));
		when(intradayClient.getIntraday("005930", LocalDate.parse("2026-06-25"), 390, true)).thenReturn(List.of(
				intraday("2026-06-25T09:01:00", "76000", "76100", "75900", "76050", 4_000L)));

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-24")
						.param("to", "2026-06-25")
						.param("interval", "30m")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.dataSource").value("KIS_TIME_DAILY_CHART_PRICE"))
				.andExpect(jsonPath("$.data.interval").value("30m"))
				.andExpect(jsonPath("$.data.pointCount").value(3))
				.andExpect(jsonPath("$.data.points[0].tradeDate").value("2026-06-24T09:00"))
				.andExpect(jsonPath("$.data.points[0].openPriceKrw").value("75000"))
				.andExpect(jsonPath("$.data.points[0].highPriceKrw").value("75300"))
				.andExpect(jsonPath("$.data.points[0].lowPriceKrw").value("75000"))
				.andExpect(jsonPath("$.data.points[0].closePriceKrw").value("75200"))
				.andExpect(jsonPath("$.data.points[0].volume").value(3000))
				.andExpect(jsonPath("$.data.points[1].tradeDate").value("2026-06-24T09:30"))
				.andExpect(jsonPath("$.data.points[2].tradeDate").value("2026-06-25T09:00"));
	}

	@Test
	void chartAggregatesMultiDayIntradayPricesForOneMonthChart() throws Exception {
		when(quoteClient.getQuote("005930", "USD")).thenReturn(quote());
		when(intradayClient.getIntraday("005930", LocalDate.parse("2026-06-24"), 390, true)).thenReturn(List.of(
				intraday("2026-06-24T09:01:00", "75000", "75200", "75000", "75100", 1_000L),
				intraday("2026-06-24T10:59:00", "75100", "75500", "75050", "75400", 2_000L),
				intraday("2026-06-24T11:01:00", "75400", "75600", "75300", "75500", 3_000L)));

		mockMvc.perform(get("/api/v1/market/stocks/005930/chart")
						.param("from", "2026-06-24")
						.param("to", "2026-06-24")
						.param("interval", "2h")
						.param("currency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.interval").value("2h"))
				.andExpect(jsonPath("$.data.pointCount").value(2))
				.andExpect(jsonPath("$.data.points[0].tradeDate").value("2026-06-24T09:00"))
				.andExpect(jsonPath("$.data.points[0].highPriceKrw").value("75500"))
				.andExpect(jsonPath("$.data.points[0].closePriceKrw").value("75400"))
				.andExpect(jsonPath("$.data.points[1].tradeDate").value("2026-06-24T11:00"));
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

	@Test
	void chartRejectsMissingRequiredDateParameters() throws Exception {
		mockMvc.perform(get("/api/v1/market/stocks/005930/chart"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"))
				.andExpect(jsonPath("$.errors[0].field").value("from"));
	}

	private OmniLensMarketHistoryPoint point(
			String tradeDate,
			String openKrw,
			String highKrw,
			String lowKrw,
			String closeKrw) {
		return new OmniLensMarketHistoryPoint(
				LocalDate.parse(tradeDate),
				new BigDecimal(openKrw),
				new BigDecimal(highKrw),
				new BigDecimal(lowKrw),
				new BigDecimal(closeKrw),
				null,
				null,
				null,
				null,
				1000000L,
				new BigDecimal("75000000000"),
				true);
	}

	private OmniLensMarketQuote quote() {
		return new OmniLensMarketQuote(
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
				new BigDecimal("0.00072"),
				null,
				"HANA_FX_RATE_API",
				false,
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				null,
				"HANA_OMNILENS_API");
	}

	private OmniLensMarketIntradayPrice intraday(
			String bucketStart,
			String openKrw,
			String highKrw,
			String lowKrw,
			String closeKrw,
			long volume) {
		return new OmniLensMarketIntradayPrice(
				"005930",
				LocalDateTime.parse(bucketStart),
				"KOSPI",
				new BigDecimal(openKrw),
				new BigDecimal(highKrw),
				new BigDecimal(lowKrw),
				new BigDecimal(closeKrw),
				volume,
				new BigDecimal("75000000000"),
				"KIS_TIME_ITEM_CHART_PRICE",
				null);
	}
}
