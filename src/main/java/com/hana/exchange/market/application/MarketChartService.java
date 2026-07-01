package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketHistoryClient;
import com.hana.exchange.market.client.OmniLensMarketHistoryPoint;
import com.hana.exchange.market.client.OmniLensMarketHistoryResponse;
import com.hana.exchange.market.client.OmniLensMarketIntradayClient;
import com.hana.exchange.market.client.OmniLensMarketIntradayPrice;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketChartPointResponse;
import com.hana.exchange.market.domain.MarketChartResponse;

@Service
public class MarketChartService {

	private final OmniLensMarketHistoryClient historyClient;
	private final OmniLensMarketIntradayClient intradayClient;
	private final OmniLensMarketQuoteClient quoteClient;

	public MarketChartService(
			OmniLensMarketHistoryClient historyClient,
			OmniLensMarketIntradayClient intradayClient,
			OmniLensMarketQuoteClient quoteClient,
			MarketIntradayCandleStore intradayCandleStore) {
		this.historyClient = historyClient;
		this.intradayClient = intradayClient;
		this.quoteClient = quoteClient;
	}

	public MarketChartResponse getChart(
			String stockCode,
			LocalDate from,
			LocalDate to,
			String interval,
			String currency) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "from must be on or before to");
		}
		if ("1m".equals(interval)) {
			return getIntradayChart(stockCode, to, currency);
		}
		OmniLensMarketHistoryResponse history = historyClient.getHistory(stockCode, from, to, interval, currency);
		OmniLensMarketQuote quote = quoteClient.getQuote(stockCode, currency);
		BigDecimal fxRate = resolveFxRate(quote);
		String localCurrency = quote.localCurrency() == null || quote.localCurrency().isBlank()
				? currency
				: quote.localCurrency();
		List<MarketChartPointResponse> points = aggregate(history.points(), interval)
				.stream()
				.map(point -> toPoint(point, localCurrency, fxRate))
				.toList();
		return new MarketChartResponse(
				history.source(),
				history.stockCode(),
				history.interval(),
				from,
				to,
				history.baseCurrency(),
				localCurrency,
				"en",
				points.size(),
				points,
				Instant.now());
	}

	private MarketChartResponse getIntradayChart(String stockCode, LocalDate date, String currency) {
		List<OmniLensMarketIntradayPrice> intradayPrices;
		try {
			intradayPrices = intradayClient.getIntraday(stockCode, date, 390);
		} catch (BusinessException exception) {
			intradayPrices = List.of();
		}
		ChartCurrency chartCurrency = chartCurrency(stockCode, currency);
		List<MarketChartPointResponse> points = intradayPrices.stream()
				.map(price -> toPoint(price, chartCurrency.localCurrency(), chartCurrency.fxRate()))
				.toList();
		return new MarketChartResponse(
				"KIS_TIME_ITEM_CHART_PRICE",
				stockCode,
				"1m",
				date,
				date,
				"KRW",
				chartCurrency.localCurrency(),
				"en",
				points.size(),
				points,
				Instant.now());
	}

	private ChartCurrency chartCurrency(String stockCode, String currency) {
		try {
			OmniLensMarketQuote quote = quoteClient.getQuote(stockCode, currency);
			BigDecimal fxRate = resolveFxRate(quote);
			String localCurrency = quote.localCurrency() == null || quote.localCurrency().isBlank()
					? currency
					: quote.localCurrency();
			return new ChartCurrency(localCurrency, fxRate);
		} catch (BusinessException exception) {
			return new ChartCurrency("KRW", BigDecimal.ONE);
		}
	}

	private List<OmniLensMarketHistoryPoint> aggregate(List<OmniLensMarketHistoryPoint> points, String interval) {
		List<OmniLensMarketHistoryPoint> sortedPoints = points.stream()
				.sorted(Comparator.comparing(OmniLensMarketHistoryPoint::tradeDate))
				.toList();
		if ("1d".equals(interval)) {
			return sortedPoints;
		}
		Map<Object, List<OmniLensMarketHistoryPoint>> grouped = new LinkedHashMap<>();
		for (OmniLensMarketHistoryPoint point : sortedPoints) {
			grouped.computeIfAbsent(intervalKey(point.tradeDate(), interval), ignored -> new java.util.ArrayList<>())
					.add(point);
		}
		return grouped.values().stream()
				.map(this::aggregateGroup)
				.toList();
	}

	private Object intervalKey(LocalDate tradeDate, String interval) {
		if ("1mo".equals(interval)) {
			return YearMonth.from(tradeDate);
		}
		WeekFields weekFields = WeekFields.of(Locale.KOREA);
		return tradeDate.get(weekFields.weekBasedYear()) + "-W" + tradeDate.get(weekFields.weekOfWeekBasedYear());
	}

	private OmniLensMarketHistoryPoint aggregateGroup(List<OmniLensMarketHistoryPoint> points) {
		OmniLensMarketHistoryPoint first = points.get(0);
		OmniLensMarketHistoryPoint last = points.get(points.size() - 1);
		return new OmniLensMarketHistoryPoint(
				first.tradeDate(),
				first.openPriceKrw(),
				max(points, OmniLensMarketHistoryPoint::highPriceKrw),
				min(points, OmniLensMarketHistoryPoint::lowPriceKrw),
				last.closePriceKrw(),
				null,
				null,
				null,
				null,
				points.stream().mapToLong(OmniLensMarketHistoryPoint::volume).sum(),
				points.stream()
						.map(OmniLensMarketHistoryPoint::tradingValueKrw)
						.reduce(BigDecimal.ZERO, BigDecimal::add),
				points.stream().anyMatch(OmniLensMarketHistoryPoint::adjusted));
	}

	private BigDecimal max(
			List<OmniLensMarketHistoryPoint> points,
			java.util.function.Function<OmniLensMarketHistoryPoint, BigDecimal> mapper) {
		return points.stream()
				.map(mapper)
				.max(BigDecimal::compareTo)
				.orElse(null);
	}

	private BigDecimal min(
			List<OmniLensMarketHistoryPoint> points,
			java.util.function.Function<OmniLensMarketHistoryPoint, BigDecimal> mapper) {
		return points.stream()
				.map(mapper)
				.min(BigDecimal::compareTo)
				.orElse(null);
	}

	private MarketChartPointResponse toPoint(OmniLensMarketHistoryPoint point, String localCurrency, BigDecimal fxRate) {
		return new MarketChartPointResponse(
				point.tradeDate().toString(),
				toText(point.openPriceKrw()),
				toText(point.highPriceKrw()),
				toText(point.lowPriceKrw()),
				toText(point.closePriceKrw()),
				localCurrency,
				toText(localPrice(point.openPriceKrw(), point.openLocalCurrencyPrice(), fxRate)),
				toText(localPrice(point.highPriceKrw(), point.highLocalCurrencyPrice(), fxRate)),
				toText(localPrice(point.lowPriceKrw(), point.lowLocalCurrencyPrice(), fxRate)),
				toText(localPrice(point.closePriceKrw(), point.closeLocalCurrencyPrice(), fxRate)),
				point.volume(),
				toText(point.tradingValueKrw()),
				point.adjusted());
	}

	private MarketChartPointResponse toPoint(OmniLensMarketIntradayPrice price, String localCurrency, BigDecimal fxRate) {
		return new MarketChartPointResponse(
				price.bucketStart().toString(),
				toText(price.openPriceKrw()),
				toText(price.highPriceKrw()),
				toText(price.lowPriceKrw()),
				toText(price.closePriceKrw()),
				localCurrency,
				toText(localPrice(price.openPriceKrw(), null, fxRate)),
				toText(localPrice(price.highPriceKrw(), null, fxRate)),
				toText(localPrice(price.lowPriceKrw(), null, fxRate)),
				toText(localPrice(price.closePriceKrw(), null, fxRate)),
				price.tradingVolume(),
				toText(price.tradingValueKrw()),
				false);
	}

	private BigDecimal localPrice(BigDecimal krwPrice, BigDecimal explicitLocalPrice, BigDecimal fxRate) {
		if (explicitLocalPrice != null) {
			return explicitLocalPrice;
		}
		if (krwPrice == null || fxRate == null) {
			return null;
		}
		return krwPrice.multiply(fxRate).setScale(4, RoundingMode.HALF_UP);
	}

	private BigDecimal resolveFxRate(OmniLensMarketQuote quote) {
		if (quote.fxRate() != null) {
			return quote.fxRate();
		}
		if (quote.currentPriceKrw() == null
				|| quote.localCurrencyPrice() == null
				|| quote.currentPriceKrw().signum() == 0) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, "Hana quote FX rate is missing");
		}
		return quote.localCurrencyPrice().divide(quote.currentPriceKrw(), 10, RoundingMode.HALF_UP);
	}

	private String toText(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}

	private record ChartCurrency(String localCurrency, BigDecimal fxRate) {
	}
}
