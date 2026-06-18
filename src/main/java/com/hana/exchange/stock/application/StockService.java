package com.hana.exchange.stock.application;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.hana.exchange.stock.client.OmniLensStockClient;
import com.hana.exchange.stock.client.OmniLensStockDetailResponse;
import com.hana.exchange.stock.client.OmniLensStockSearchItem;
import com.hana.exchange.stock.client.OmniLensStockSearchResponse;
import com.hana.exchange.stock.domain.StockDetailResponse;
import com.hana.exchange.stock.domain.StockSearchItemResponse;
import com.hana.exchange.stock.domain.StockSearchResponse;

@Service
public class StockService {

	private final OmniLensStockClient stockClient;

	public StockService(OmniLensStockClient stockClient) {
		this.stockClient = stockClient;
	}

	public StockSearchResponse search(String query, String market, String currency, int limit) {
		OmniLensStockSearchResponse response = stockClient.search(query, market, currency, limit);
		var results = response.results().stream()
				.map(this::toSearchItem)
				.toList();
		return new StockSearchResponse(
				response.query(),
				response.market(),
				"en",
				response.currency(),
				results.size(),
				results,
				Instant.now());
	}

	public StockDetailResponse getDetail(String stockCode, String currency) {
		OmniLensStockDetailResponse detail = stockClient.getDetail(stockCode, currency);
		return new StockDetailResponse(
				detail.stockCode(),
				displayName(detail.stockNameEn(), detail.stockName()),
				detail.market(),
				detail.sector(),
				"en",
				"KRW",
				detail.localCurrency(),
				toText(detail.currentPriceKrw()),
				toText(detail.localCurrencyPrice()),
				toText(detail.changeRate()),
				detail.volume(),
				detail.marketDataTime(),
				detail.foreignOwnedQuantity(),
				toText(detail.foreignOwnershipRate()),
				toText(detail.foreignLimitExhaustionRate()),
				detail.foreignOwnershipBaseDate(),
				detail.viActive(),
				detail.priceLimitState(),
				detail.tradingHalted(),
				detail.orderable(),
				detail.source(),
				Instant.now());
	}

	private StockSearchItemResponse toSearchItem(OmniLensStockSearchItem item) {
		return new StockSearchItemResponse(
				item.stockCode(),
				displayName(item.stockNameEn(), item.stockName()),
				item.market(),
				item.sector(),
				item.source());
	}

	private String displayName(String stockNameEn, String stockName) {
		if (stockNameEn != null && !stockNameEn.isBlank()) {
			return stockNameEn;
		}
		return stockName;
	}

	private String toText(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}
}
