package com.hana.exchange.watchlist.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.watchlist.domain.WatchlistItem;

@Repository
@Profile("memory")
public class InMemoryWatchlistRepository implements WatchlistRepository {

	private final Map<String, WatchlistItem> itemsByAccountAndStock = new ConcurrentHashMap<>();

	@Override
	public List<WatchlistItem> findItems(String accountId) {
		return itemsByAccountAndStock.values()
				.stream()
				.filter(item -> item.accountId().equals(accountId))
				.sorted(Comparator.comparing(WatchlistItem::addedAt))
				.toList();
	}

	@Override
	public List<WatchlistItem> findItemsByStockCodes(List<String> stockCodes) {
		return itemsByAccountAndStock.values()
				.stream()
				.filter(item -> stockCodes.contains(item.stockCode()))
				.sorted(Comparator.comparing(WatchlistItem::accountId).thenComparing(WatchlistItem::stockCode))
				.toList();
	}

	@Override
	public Optional<WatchlistItem> findItem(String accountId, String stockCode) {
		return Optional.ofNullable(itemsByAccountAndStock.get(key(accountId, stockCode)));
	}

	@Override
	public void saveItem(WatchlistItem item) {
		itemsByAccountAndStock.put(key(item.accountId(), item.stockCode()), item);
	}

	@Override
	public boolean deleteItem(String accountId, String stockCode) {
		return itemsByAccountAndStock.remove(key(accountId, stockCode)) != null;
	}

	private String key(String accountId, String stockCode) {
		return accountId + ":" + stockCode;
	}
}
