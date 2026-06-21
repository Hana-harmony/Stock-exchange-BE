package com.hana.exchange.alert.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventMatchResult;
import com.hana.exchange.alert.domain.StockIntelligenceFeedItemResponse;
import com.hana.exchange.alert.domain.StockIntelligenceFeedResponse;

@Service
public class StockIntelligenceFeedService {

	private final AlertEventRepository alertEventRepository;

	public StockIntelligenceFeedService(AlertEventRepository alertEventRepository) {
		this.alertEventRepository = alertEventRepository;
	}

	public StockIntelligenceFeedResponse getFeed(String stockCode) {
		List<StockIntelligenceFeedItemResponse> items = alertEventRepository.findByStockCode(stockCode)
				.stream()
				.map(this::toFeedItem)
				.toList();
		return new StockIntelligenceFeedResponse(
				stockCode,
				"HANA_OMNILENS_AI_ANALYZED_EVENT",
				items.size(),
				items,
				Instant.now());
	}

	private StockIntelligenceFeedItemResponse toFeedItem(AlertEventMatchResult result) {
		AlertEvent event = result.event();
		return new StockIntelligenceFeedItemResponse(
				event.eventId(),
				event.sourceType(),
				event.title(),
				event.summary(),
				event.summaryLines(),
				event.translatedSummary(),
				event.originalContent(),
				event.translatedContent(),
				event.imageUrls(),
				event.contentAvailability(),
				event.originalUrl(),
				event.stockCode(),
				event.relatedStocks(),
				event.sentiment(),
				event.importance(),
				event.riskLevel(),
				event.clusterKey(),
				event.glossaryTerms(),
				event.translationQualityFlags(),
				event.watchlistTarget(),
				event.holderTarget(),
				event.publishedAt(),
				event.receivedAt(),
				result.targetCount());
	}
}
