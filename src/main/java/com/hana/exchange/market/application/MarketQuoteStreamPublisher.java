package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hana.exchange.market.domain.MarketQuoteStreamPublishResponse;
import com.hana.exchange.market.domain.MarketQuoteTickMessage;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;
import com.hana.exchange.stock.application.StockDisplayNameFormatter;
import com.hana.exchange.trade.application.TradeRepository;
import com.hana.exchange.trade.application.TradeService;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.watchlist.application.WatchlistRepository;
import com.hana.exchange.watchlist.domain.WatchlistItem;

@Service
public class MarketQuoteStreamPublisher {

	private static final String TRANSPORT = "WebSocket";

	private final SimpMessagingTemplate messagingTemplate;
	private final WatchlistRepository watchlistRepository;
	private final TradeRepository tradeRepository;
	private final TradeService tradeService;
	private final MarketIntradayCandleStore intradayCandleStore;

	public MarketQuoteStreamPublisher(
			SimpMessagingTemplate messagingTemplate,
			WatchlistRepository watchlistRepository,
			TradeRepository tradeRepository,
			TradeService tradeService,
			MarketIntradayCandleStore intradayCandleStore) {
		this.messagingTemplate = messagingTemplate;
		this.watchlistRepository = watchlistRepository;
		this.tradeRepository = tradeRepository;
		this.tradeService = tradeService;
		this.intradayCandleStore = intradayCandleStore;
	}

	public MarketQuoteStreamPublishResponse publish(MarketQuoteTickRequest request) {
		Instant now = Instant.now();
		intradayCandleStore.record(request);
		tradeService.processLimitOrders(request);
		MarketQuoteTickMessage message = toMessage(request, now);
		List<String> topics = topics(request);
		topics.forEach(topic -> messagingTemplate.convertAndSend(topic, message));
		return new MarketQuoteStreamPublishResponse(
				request.stockCode(),
				request.market(),
				topics.size(),
				topics,
				now);
	}

	private List<String> topics(MarketQuoteTickRequest request) {
		Set<String> topics = new LinkedHashSet<>();
		topics.add("/topic/market/quotes");
		topics.add("/topic/market/markets/" + request.market());
		topics.add("/topic/market/stocks/" + request.stockCode());
		watchlistRepository.findItemsByStockCodes(List.of(request.stockCode()))
				.stream()
				.map(WatchlistItem::accountId)
				.distinct()
				.map(accountId -> "/topic/accounts/" + accountId + "/market/quotes/watchlist")
				.forEach(topics::add);
		tradeRepository.findHoldingsByStockCodes(List.of(request.stockCode()))
				.stream()
				.map(MockHolding::accountId)
				.distinct()
				.map(accountId -> "/topic/accounts/" + accountId + "/market/quotes/portfolio")
				.forEach(topics::add);
		return List.copyOf(topics);
	}

	private MarketQuoteTickMessage toMessage(MarketQuoteTickRequest request, Instant publishedAt) {
		return new MarketQuoteTickMessage(
				request.stockCode(),
				displayName(request.stockNameEn(), request.stockName(), request.stockCode()),
				request.market(),
				toText(request.currentPriceKrw()),
				toText(request.changeRate()),
				request.volume(),
				request.marketSession(),
				toText(request.afterHoursPriceKrw()),
				toText(request.afterHoursLocalCurrencyPrice()),
				toText(request.afterHoursChangeRate()),
				request.afterHoursVolume(),
				request.afterHoursMarketDataTime(),
				request.localCurrency(),
				toText(request.localCurrencyPrice()),
				toText(request.fxRate()),
				request.fxRateTime(),
				request.fxRateSource(),
				request.fxStale(),
				request.marketDataTime(),
				request.source(),
				TRANSPORT,
				publishedAt);
	}

	private String toText(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}

	private String displayName(String stockNameEn, String stockName, String fallback) {
		return StockDisplayNameFormatter.displayName(stockNameEn, stockName, fallback);
	}
}
