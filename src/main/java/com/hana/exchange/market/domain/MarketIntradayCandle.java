package com.hana.exchange.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketIntradayCandle(
		String stockCode,
		String market,
		Instant bucketStart,
		BigDecimal openPriceKrw,
		BigDecimal highPriceKrw,
		BigDecimal lowPriceKrw,
		BigDecimal closePriceKrw,
		long volume,
		String source
) {
	public MarketIntradayCandle merge(MarketQuoteTickRequest request, Instant bucketStart) {
		BigDecimal price = request.currentPriceKrw();
		return new MarketIntradayCandle(
				stockCode,
				market,
				bucketStart,
				openPriceKrw,
				highPriceKrw.max(price),
				lowPriceKrw.min(price),
				price,
				Math.max(volume, request.volume()),
				request.source());
	}
}
