package com.hana.exchange.stock.client;

import java.util.List;

public record OmniLensStockSearchResponse(
		String query,
		String market,
		String currency,
		List<OmniLensStockSearchItem> results,
		String source
) {
}
