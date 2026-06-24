package com.hana.exchange.watchlist.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.stock.application.StockDisplayNameFormatter;
import com.hana.exchange.watchlist.domain.WatchlistAddRequest;
import com.hana.exchange.watchlist.domain.WatchlistItem;
import com.hana.exchange.watchlist.domain.WatchlistItemResponse;
import com.hana.exchange.watchlist.domain.WatchlistResponse;

@Service
public class WatchlistService {

	private static final String USD = "USD";
	private static final String TARGETING_MODE = "WATCHLIST_ALERT_TARGET";

	private final AccountRepository accountRepository;
	private final WatchlistRepository watchlistRepository;
	private final OmniLensMarketQuoteClient quoteClient;

	public WatchlistService(
			AccountRepository accountRepository,
			WatchlistRepository watchlistRepository,
			OmniLensMarketQuoteClient quoteClient) {
		this.accountRepository = accountRepository;
		this.watchlistRepository = watchlistRepository;
		this.quoteClient = quoteClient;
	}

	public WatchlistResponse getWatchlist(String accountId) {
		MockUsdAccount account = account(accountId);
		return response(account);
	}

	public WatchlistResponse addItem(String accountId, WatchlistAddRequest request) {
		MockUsdAccount account = account(accountId);
		WatchlistItem item = watchlistRepository.findItem(accountId, request.stockCode())
				.orElseGet(() -> newItem(account, request.stockCode()));
		watchlistRepository.saveItem(item);
		return response(account);
	}

	public WatchlistResponse removeItem(String accountId, String stockCode) {
		MockUsdAccount account = account(accountId);
		if (!watchlistRepository.deleteItem(accountId, stockCode)) {
			throw new BusinessException(ErrorCode.WATCHLIST_ITEM_NOT_FOUND);
		}
		return response(account);
	}

	private WatchlistItem newItem(MockUsdAccount account, String stockCode) {
		OmniLensMarketQuote quote = quoteClient.getQuote(stockCode, USD);
		Instant now = Instant.now();
		return new WatchlistItem(
				account.accountId(),
				account.userId(),
				quote.stockCode(),
				displayName(quote),
				quote.market(),
				TARGETING_MODE,
				now);
	}

	private WatchlistResponse response(MockUsdAccount account) {
		List<WatchlistItemResponse> items = watchlistRepository.findItems(account.accountId())
				.stream()
				.map(this::toResponse)
				.toList();
		return new WatchlistResponse(
				account.userId(),
				account.accountId(),
				items.size(),
				TARGETING_MODE,
				items,
				Instant.now());
	}

	private WatchlistItemResponse toResponse(WatchlistItem item) {
		return new WatchlistItemResponse(
				item.stockCode(),
				item.stockName(),
				item.market(),
				item.targetingMode(),
				item.addedAt());
	}

	private MockUsdAccount account(String accountId) {
		return accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
	}

	private String displayName(OmniLensMarketQuote quote) {
		return StockDisplayNameFormatter.displayName(quote.stockNameEn(), quote.stockName());
	}
}
