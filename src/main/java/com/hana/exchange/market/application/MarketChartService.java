package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketIntradayCandle;
import com.hana.exchange.market.domain.MarketChartPointResponse;
import com.hana.exchange.market.domain.MarketChartResponse;

@Service
public class MarketChartService {

	private final OmniLensMarketHistoryClient historyClient;
	private final OmniLensMarketQuoteClient quoteClient;
	private final MarketIntradayCandleStore intradayCandleStore;

	public MarketChartService(
			OmniLensMarketHistoryClient historyClient,
			OmniLensMarketQuoteClient quoteClient,
			MarketIntradayCandleStore intradayCandleStore) {
		this.historyClient = historyClient;
		this.quoteClient = quoteClient;
		this.intradayCandleStore = intradayCandleStore;
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
		if ("1d".equals(interval)) {
			List<MarketIntradayCandle> intradayCandles = intradayCandles(stockCode, from, to);
			if (!intradayCandles.isEmpty()) {
				OmniLensMarketQuote quote = quoteClient.getQuote(stockCode, currency);
				BigDecimal fxRate = resolveFxRate(quote);
				String localCurrency = quote.localCurrency() == null || quote.localCurrency().isBlank()
						? currency
						: quote.localCurrency();
				List<MarketChartPointResponse> intradayPoints = intradayCandles.stream()
						.map(candle -> toPoint(candle, localCurrency, fxRate))
						.toList();
				return new MarketChartResponse(
						"STOCK_EXCHANGE_INTRADAY_CANDLE",
						stockCode,
						interval,
						from,
						to,
						"KRW",
						localCurrency,
						"en",
						intradayPoints.size(),
						intradayPoints,
						Instant.now());
			}
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

	private List<MarketIntradayCandle> intradayCandles(
			String stockCode,
			LocalDate from,
			LocalDate to) {
		ZoneId koreaZone = ZoneId.of("Asia/Seoul");
		Instant fromInclusive = from.atStartOfDay(koreaZone).toInstant();
		Instant toExclusive = to.plusDays(1).atStartOfDay(koreaZone).toInstant();
		return intradayCandleStore.find(stockCode, fromInclusive, toExclusive, 480);
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

	private MarketChartPointResponse toPoint(MarketIntradayCandle candle, String localCurrency, BigDecimal fxRate) {
		return new MarketChartPointResponse(
				candle.bucketStart().toString(),
				toText(candle.openPriceKrw()),
				toText(candle.highPriceKrw()),
				toText(candle.lowPriceKrw()),
				toText(candle.closePriceKrw()),
				localCurrency,
				toText(localPrice(candle.openPriceKrw(), null, fxRate)),
				toText(localPrice(candle.highPriceKrw(), null, fxRate)),
				toText(localPrice(candle.lowPriceKrw(), null, fxRate)),
				toText(localPrice(candle.closePriceKrw(), null, fxRate)),
				candle.volume(),
				null,
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
}
