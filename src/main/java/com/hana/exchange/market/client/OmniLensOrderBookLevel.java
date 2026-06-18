package com.hana.exchange.market.client;

import java.math.BigDecimal;

public record OmniLensOrderBookLevel(
		BigDecimal priceKrw,
		BigDecimal localCurrencyPrice,
		long quantity,
		long orderCount
) {
}
