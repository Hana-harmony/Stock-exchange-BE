package com.hana.exchange.market.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Component;

import com.hana.exchange.market.domain.MarketIntradayCandle;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;

@Component
public class MarketIntradayCandleStore {

	private static final int MAX_CANDLES_PER_STOCK = 480;

	private final ConcurrentHashMap<String, NavigableMap<Instant, MarketIntradayCandle>> candlesByStockCode =
			new ConcurrentHashMap<>();

	public void record(MarketQuoteTickRequest request) {
		Instant bucketStart = request.marketDataTime().truncatedTo(ChronoUnit.MINUTES);
		NavigableMap<Instant, MarketIntradayCandle> candles = candlesByStockCode.computeIfAbsent(
				request.stockCode(),
				ignored -> new ConcurrentSkipListMap<>());
		candles.compute(bucketStart, (ignored, existing) -> existing == null
				? new MarketIntradayCandle(
						request.stockCode(),
						request.market(),
						bucketStart,
						request.currentPriceKrw(),
						request.currentPriceKrw(),
						request.currentPriceKrw(),
						request.currentPriceKrw(),
						request.volume(),
						request.source())
				: existing.merge(request, bucketStart));
		trim(candles);
	}

	public List<MarketIntradayCandle> find(String stockCode, Instant fromInclusive, Instant toExclusive, int limit) {
		NavigableMap<Instant, MarketIntradayCandle> candles = candlesByStockCode.get(stockCode);
		if (candles == null || candles.isEmpty()) {
			return List.of();
		}
		return candles.subMap(fromInclusive, true, toExclusive, false)
				.values()
				.stream()
				.sorted(Comparator.comparing(MarketIntradayCandle::bucketStart))
				.limit(Math.max(1, limit))
				.toList();
	}

	public void clear() {
		candlesByStockCode.clear();
	}

	private void trim(NavigableMap<Instant, MarketIntradayCandle> candles) {
		while (candles.size() > MAX_CANDLES_PER_STOCK) {
			candles.pollFirstEntry();
		}
	}
}
