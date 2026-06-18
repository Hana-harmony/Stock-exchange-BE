package com.hana.exchange.watchlist.application;

import java.util.List;
import java.util.Optional;

import com.hana.exchange.watchlist.domain.WatchlistItem;

public interface WatchlistRepository {

	List<WatchlistItem> findItems(String accountId);

	List<WatchlistItem> findItemsByStockCodes(List<String> stockCodes);

	Optional<WatchlistItem> findItem(String accountId, String stockCode);

	void saveItem(WatchlistItem item);

	boolean deleteItem(String accountId, String stockCode);
}
