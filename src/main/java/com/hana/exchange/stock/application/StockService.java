package com.hana.exchange.stock.application;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.hana.exchange.stock.client.OmniLensStockClient;
import com.hana.exchange.stock.client.OmniLensGlobalPeerMatch;
import com.hana.exchange.stock.client.OmniLensGlobalPeerResponse;
import com.hana.exchange.stock.client.OmniLensStockDetailResponse;
import com.hana.exchange.stock.client.OmniLensStockSearchItem;
import com.hana.exchange.stock.client.OmniLensStockSearchResponse;
import com.hana.exchange.stock.domain.GlobalPeerMatchResponse;
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
				toText(fallback(detail.predictedForeignOwnershipRateMin(), detail.foreignOwnershipRate())),
				toText(fallback(detail.predictedForeignOwnershipRateMax(), detail.foreignOwnershipRate())),
				toText(fallback(detail.predictedForeignLimitExhaustionRateMin(), detail.foreignLimitExhaustionRate())),
				toText(fallback(detail.predictedForeignLimitExhaustionRateMax(), detail.foreignLimitExhaustionRate())),
				detail.foreignOwnershipPredictionConfidenceLevel(),
				toText(detail.foreignOwnershipPredictionConfidenceScore()),
				detail.foreignOwnershipPredictionModelVersion(),
				detail.foreignOwnershipBaseDate(),
				detail.viActive(),
				detail.singlePriceTrading(),
				normalizePriceLimitState(detail.priceLimitState()),
				detail.tradingHalted(),
				detail.orderable(),
				detail.source(),
				Instant.now());
	}

	public GlobalPeerMatchResponse getGlobalPeers(String stockCode) {
		OmniLensGlobalPeerResponse response = stockClient.getGlobalPeers(stockCode);
		return new GlobalPeerMatchResponse(
				response.stockCode(),
				displayName(response.stockNameEn(), response.stockName()),
				response.headline(),
				response.summary(),
				toPeer(response.primaryPeer()),
				response.peers().stream().map(this::toPeer).toList(),
				toText(response.confidenceScore()),
				response.confidenceLevel(),
				response.modelVersion(),
				response.source(),
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

	private GlobalPeerMatchResponse.Peer toPeer(OmniLensGlobalPeerMatch peer) {
		return new GlobalPeerMatchResponse.Peer(
				peer.rank(),
				peer.ticker(),
				peer.companyName(),
				peer.exchange(),
				peer.country(),
				toText(peer.similarityScore()),
				peer.businessTags(),
				peer.sector(),
				peer.industry(),
				peer.businessModel(),
				peer.scaleBucket(),
				peer.fiscalYear(),
				toText(peer.marketCapUsd()),
				toText(peer.revenueUsd()),
				toText(peer.operatingIncomeUsd()),
				toText(peer.netIncomeUsd()),
				peer.financialDataSource(),
				toText(peer.financialSimilarityScore()),
				peer.matchedFactors(),
				peer.rationale());
	}

	private String displayName(String stockNameEn, String stockName) {
		return StockDisplayNameFormatter.displayName(stockNameEn, stockName);
	}

	private String toText(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}

	private BigDecimal fallback(BigDecimal value, BigDecimal fallback) {
		return value == null ? fallback : value;
	}

	private String normalizePriceLimitState(String priceLimitState) {
		if (priceLimitState == null || priceLimitState.isBlank()) {
			return "NORMAL";
		}
		return switch (priceLimitState.trim().toUpperCase()) {
			case "UPPER", "UPPER_LIMIT" -> "UPPER";
			case "LOWER", "LOWER_LIMIT" -> "LOWER";
			case "NORMAL" -> "NORMAL";
			default -> "NORMAL";
		};
	}
}
