package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
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

	private static final LocalTime REGULAR_MARKET_OPEN = LocalTime.of(9, 0);
	private static final LocalTime REGULAR_MARKET_CLOSE = LocalTime.of(15, 30);

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
		if ("30m".equals(interval) || "2h".equals(interval)) {
			return getIntradayRangeChart(stockCode, from, to, interval, currency);
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
				.filter(this::isRegularSessionPrice)
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

	private MarketChartResponse getIntradayRangeChart(
			String stockCode,
			LocalDate from,
			LocalDate to,
			String interval,
			String currency) {
		Duration bucketSize = "2h".equals(interval) ? Duration.ofHours(2) : Duration.ofMinutes(30);
		boolean fetchMissing = !"2h".equals(interval);
		List<OmniLensMarketIntradayPrice> prices = new ArrayList<>();
		LocalDate date = from;
		while (!date.isAfter(to)) {
			if (isKoreanTradingWeekday(date)) {
				try {
					prices.addAll(intradayClient.getIntraday(stockCode, date, 390, fetchMissing));
				} catch (BusinessException exception) {
					// 휴장일이나 provider 일시 실패는 다른 거래일 차트를 막지 않는다.
				}
			}
			date = date.plusDays(1);
		}
		ChartCurrency chartCurrency = chartCurrency(stockCode, currency);
		List<MarketChartPointResponse> points = aggregateIntraday(prices, bucketSize)
				.stream()
				.map(price -> toPoint(price, chartCurrency.localCurrency(), chartCurrency.fxRate()))
				.toList();
		return new MarketChartResponse(
				"KIS_TIME_DAILY_CHART_PRICE",
				stockCode,
				interval,
				from,
				to,
				"KRW",
				chartCurrency.localCurrency(),
				"en",
				points.size(),
				points,
				Instant.now());
	}

	private boolean isKoreanTradingWeekday(LocalDate date) {
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
	}

	private boolean isRegularSessionPrice(OmniLensMarketIntradayPrice price) {
		LocalTime time = price.bucketStart().toLocalTime();
		return !time.isBefore(REGULAR_MARKET_OPEN) && !time.isAfter(REGULAR_MARKET_CLOSE);
	}

	private List<OmniLensMarketIntradayPrice> aggregateIntraday(
			List<OmniLensMarketIntradayPrice> prices,
			Duration bucketSize) {
		List<OmniLensMarketIntradayPrice> sortedPrices = prices.stream()
				.filter(this::isRegularSessionPrice)
				.sorted(Comparator.comparing(OmniLensMarketIntradayPrice::bucketStart))
				.toList();
		Map<LocalDateTime, List<OmniLensMarketIntradayPrice>> grouped = new LinkedHashMap<>();
		for (OmniLensMarketIntradayPrice price : sortedPrices) {
			grouped.computeIfAbsent(intradayBucket(price.bucketStart(), bucketSize), ignored -> new ArrayList<>())
					.add(price);
		}
		return grouped.entrySet().stream()
				.map(entry -> aggregateIntradayGroup(entry.getKey(), entry.getValue()))
				.toList();
	}

	private LocalDateTime intradayBucket(LocalDateTime bucketStart, Duration bucketSize) {
		int minutesFromOpen = Math.max(0, (int) Duration.between(
				LocalDateTime.of(bucketStart.toLocalDate(), REGULAR_MARKET_OPEN),
				bucketStart).toMinutes());
		int bucketMinutes = Math.max(1, (int) bucketSize.toMinutes());
		int normalizedMinutes = (minutesFromOpen / bucketMinutes) * bucketMinutes;
		return LocalDateTime.of(bucketStart.toLocalDate(), REGULAR_MARKET_OPEN).plusMinutes(normalizedMinutes);
	}

	private OmniLensMarketIntradayPrice aggregateIntradayGroup(
			LocalDateTime bucketStart,
			List<OmniLensMarketIntradayPrice> prices) {
		OmniLensMarketIntradayPrice first = prices.get(0);
		OmniLensMarketIntradayPrice last = prices.get(prices.size() - 1);
		return new OmniLensMarketIntradayPrice(
				first.stockCode(),
				bucketStart,
				first.market(),
				first.openPriceKrw(),
				maxIntraday(prices, OmniLensMarketIntradayPrice::highPriceKrw),
				minIntraday(prices, OmniLensMarketIntradayPrice::lowPriceKrw),
				last.closePriceKrw(),
				prices.stream().mapToLong(OmniLensMarketIntradayPrice::tradingVolume).sum(),
				prices.stream()
						.map(OmniLensMarketIntradayPrice::tradingValueKrw)
						.filter(value -> value != null)
						.reduce(BigDecimal.ZERO, BigDecimal::add),
				first.source(),
				Instant.now());
	}

	private BigDecimal maxIntraday(
			List<OmniLensMarketIntradayPrice> prices,
			java.util.function.Function<OmniLensMarketIntradayPrice, BigDecimal> mapper) {
		return prices.stream()
				.map(mapper)
				.max(BigDecimal::compareTo)
				.orElse(null);
	}

	private BigDecimal minIntraday(
			List<OmniLensMarketIntradayPrice> prices,
			java.util.function.Function<OmniLensMarketIntradayPrice, BigDecimal> mapper) {
		return prices.stream()
				.map(mapper)
				.min(BigDecimal::compareTo)
				.orElse(null);
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
