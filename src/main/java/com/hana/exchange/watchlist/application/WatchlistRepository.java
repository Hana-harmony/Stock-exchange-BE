package com.hana.exchange.watchlist.application;

import java.util.List;
import java.util.Optional;

import com.hana.exchange.watchlist.domain.WatchlistItem;

public interface WatchlistRepository {

	List<WatchlistItem> findItems(String accountId);

	Optional<WatchlistItem> findItem(String accountId, String stockCode);

	void saveItem(WatchlistItem item);

	boolean deleteItem(String accountId, String stockCode);
}
