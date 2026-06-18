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
import com.hana.exchange.trade.application.TradeRepository;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.watchlist.application.WatchlistRepository;
import com.hana.exchange.watchlist.domain.WatchlistItem;

@Service
public class MarketQuoteStreamPublisher {

	private static final String TRANSPORT = "WebSocket";

	private final SimpMessagingTemplate messagingTemplate;
	private final WatchlistRepository watchlistRepository;
	private final TradeRepository tradeRepository;

	public MarketQuoteStreamPublisher(
			SimpMessagingTemplate messagingTemplate,
			WatchlistRepository watchlistRepository,
			TradeRepository tradeRepository) {
		this.messagingTemplate = messagingTemplate;
		this.watchlistRepository = watchlistRepository;
		this.tradeRepository = tradeRepository;
	}

	public MarketQuoteStreamPublishResponse publish(MarketQuoteTickRequest request) {
		Instant now = Instant.now();
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
				request.stockName(),
				request.market(),
				toText(request.currentPriceKrw()),
				toText(request.changeRate()),
				request.volume(),
				request.localCurrency(),
				toText(request.localCurrencyPrice()),
				toText(request.fxRate()),
				request.fxRateTime(),
				request.fxStale(),
				request.marketDataTime(),
				request.source(),
				TRANSPORT,
				publishedAt);
	}

	private String toText(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}
}
