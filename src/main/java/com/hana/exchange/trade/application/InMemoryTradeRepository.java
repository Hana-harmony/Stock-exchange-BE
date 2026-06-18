package com.hana.exchange.trade.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;

@Repository
@Profile("memory")
public class InMemoryTradeRepository implements TradeRepository {

	private final Map<String, MockHolding> holdingsByAccountAndStock = new ConcurrentHashMap<>();
	private final Map<String, MockTradeLedgerEntry> tradesById = new ConcurrentHashMap<>();

	@Override
	public Optional<MockHolding> findHolding(String accountId, String stockCode) {
		return Optional.ofNullable(holdingsByAccountAndStock.get(key(accountId, stockCode)));
	}

	@Override
	public List<MockHolding> findHoldings(String accountId) {
		return holdingsByAccountAndStock.values()
				.stream()
				.filter(holding -> holding.accountId().equals(accountId))
				.sorted(Comparator.comparing(MockHolding::stockCode))
				.toList();
	}

	@Override
	public List<MockHolding> findHoldingsByStockCodes(List<String> stockCodes) {
		return holdingsByAccountAndStock.values()
				.stream()
				.filter(holding -> stockCodes.contains(holding.stockCode()))
				.sorted(Comparator.comparing(MockHolding::accountId).thenComparing(MockHolding::stockCode))
				.toList();
	}

	@Override
	public void saveHolding(MockHolding holding) {
		String key = key(holding.accountId(), holding.stockCode());
		if (holding.quantity() == 0) {
			holdingsByAccountAndStock.remove(key);
			return;
		}
		holdingsByAccountAndStock.put(key, holding);
	}

	@Override
	public void saveTrade(MockTradeLedgerEntry trade) {
		tradesById.put(trade.tradeId(), trade);
	}

	@Override
	public List<MockTradeLedgerEntry> findTrades(String accountId) {
		return tradesById.values()
				.stream()
				.filter(trade -> trade.accountId().equals(accountId))
				.sorted(Comparator.comparing(MockTradeLedgerEntry::executedAt))
				.toList();
	}

	@Override
	public List<MockTradeLedgerEntry> findRecentTrades(String accountId, int limit) {
		List<MockTradeLedgerEntry> trades = new ArrayList<>(findTrades(accountId));
		trades.sort(Comparator.comparing(MockTradeLedgerEntry::executedAt).reversed());
		return trades.stream().limit(limit).toList();
	}

	private String key(String accountId, String stockCode) {
		return accountId + ":" + stockCode;
	}
}
