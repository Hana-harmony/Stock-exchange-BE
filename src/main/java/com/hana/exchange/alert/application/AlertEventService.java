package com.hana.exchange.alert.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventIngestRequest;
import com.hana.exchange.alert.domain.AlertEventMatchResponse;
import com.hana.exchange.alert.domain.AlertEventMatchResult;
import com.hana.exchange.alert.domain.AlertTargetResponse;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.notification.application.NotificationService;
import com.hana.exchange.trade.application.TradeRepository;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.watchlist.application.WatchlistRepository;
import com.hana.exchange.watchlist.domain.WatchlistItem;

@Service
public class AlertEventService {

	private final AlertEventRepository alertEventRepository;
	private final WatchlistRepository watchlistRepository;
	private final TradeRepository tradeRepository;
	private final NotificationService notificationService;

	public AlertEventService(
			AlertEventRepository alertEventRepository,
			WatchlistRepository watchlistRepository,
			TradeRepository tradeRepository,
			NotificationService notificationService) {
		this.alertEventRepository = alertEventRepository;
		this.watchlistRepository = watchlistRepository;
		this.tradeRepository = tradeRepository;
		this.notificationService = notificationService;
	}

	public AlertEventMatchResponse ingest(AlertEventIngestRequest request) {
		return alertEventRepository.findByIdempotencyKey(request.idempotencyKey())
				.map(AlertEventMatchResult::toResponse)
				.orElseGet(() -> ingestNew(request));
	}

	public AlertEventMatchResponse getTargets(String eventId) {
		return alertEventRepository.findByEventId(eventId)
				.map(AlertEventMatchResult::toResponse)
				.orElseThrow(() -> new BusinessException(ErrorCode.ALERT_EVENT_NOT_FOUND));
	}

	private AlertEventMatchResponse ingestNew(AlertEventIngestRequest request) {
		AlertEvent event = new AlertEvent(
				request.eventId(),
				request.idempotencyKey(),
				request.sourceType(),
				request.title(),
				request.summary(),
				request.originalUrl(),
				request.stockCode(),
				request.relatedStocks(),
				request.glossaryTerms(),
				request.translationQualityFlags(),
				request.sentiment(),
				request.importance(),
				request.riskLevel(),
				request.watchlistTarget(),
				request.holderTarget(),
				request.publishedAt(),
				Instant.now());
		AlertEventMatchResult matchResult = new AlertEventMatchResult(event, targets(event), Instant.now());
		alertEventRepository.save(event, matchResult);
		notificationService.storeAlertNotifications(event, matchResult.targets());
		return matchResult.toResponse();
	}

	private List<AlertTargetResponse> targets(AlertEvent event) {
		List<String> stockCodes = event.matchingStockCodes();
		Map<String, TargetAccumulator> targetsByAccount = new LinkedHashMap<>();
		if (event.watchlistTarget()) {
			for (WatchlistItem item : watchlistRepository.findItemsByStockCodes(stockCodes)) {
				targetsByAccount.computeIfAbsent(item.accountId(), key -> new TargetAccumulator(item.accountId(), item.userId()))
						.add("WATCHLIST", item.stockCode());
			}
		}
		if (event.holderTarget()) {
			for (MockHolding holding : tradeRepository.findHoldingsByStockCodes(stockCodes)) {
				targetsByAccount.computeIfAbsent(holding.accountId(), key -> new TargetAccumulator(holding.accountId(), holding.userId()))
						.add("HOLDER", holding.stockCode());
			}
		}
		return targetsByAccount.values()
				.stream()
				.map(TargetAccumulator::toResponse)
				.toList();
	}

	private static final class TargetAccumulator {

		private final String accountId;
		private final String userId;
		private final LinkedHashSet<String> matchReasons = new LinkedHashSet<>();
		private final LinkedHashSet<String> matchedStockCodes = new LinkedHashSet<>();

		private TargetAccumulator(String accountId, String userId) {
			this.accountId = accountId;
			this.userId = userId;
		}

		private void add(String reason, String stockCode) {
			matchReasons.add(reason);
			matchedStockCodes.add(stockCode);
		}

		private AlertTargetResponse toResponse() {
			return new AlertTargetResponse(
					accountId,
					userId,
					new ArrayList<>(matchReasons),
					new ArrayList<>(matchedStockCodes));
		}
	}
}
