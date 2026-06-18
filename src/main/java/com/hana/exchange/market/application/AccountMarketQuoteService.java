package com.hana.exchange.market.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;
import com.hana.exchange.trade.application.TradeRepository;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.watchlist.application.WatchlistRepository;
import com.hana.exchange.watchlist.domain.WatchlistItem;

@Service
public class AccountMarketQuoteService {

	private static final String WATCHLIST_COVERAGE = "WATCHLIST_STOCKS";
	private static final String PORTFOLIO_COVERAGE = "PORTFOLIO_HOLDINGS";

	private final AccountRepository accountRepository;
	private final WatchlistRepository watchlistRepository;
	private final TradeRepository tradeRepository;
	private final MarketQuoteService marketQuoteService;

	public AccountMarketQuoteService(
			AccountRepository accountRepository,
			WatchlistRepository watchlistRepository,
			TradeRepository tradeRepository,
			MarketQuoteService marketQuoteService) {
		this.accountRepository = accountRepository;
		this.watchlistRepository = watchlistRepository;
		this.tradeRepository = tradeRepository;
		this.marketQuoteService = marketQuoteService;
	}

	public MarketQuoteSnapshot getWatchlistQuotes(String accountId, String market, String currency) {
		MockUsdAccount account = account(accountId);
		List<String> stockCodes = watchlistRepository.findItems(account.accountId())
				.stream()
				.map(WatchlistItem::stockCode)
				.toList();
		return marketQuoteService.getQuoteSnapshotForScope(stockCodes, market, currency, WATCHLIST_COVERAGE);
	}

	public MarketQuoteSnapshot getPortfolioQuotes(String accountId, String market, String currency) {
		MockUsdAccount account = account(accountId);
		List<String> stockCodes = tradeRepository.findHoldings(account.accountId())
				.stream()
				.map(MockHolding::stockCode)
				.toList();
		return marketQuoteService.getQuoteSnapshotForScope(stockCodes, market, currency, PORTFOLIO_COVERAGE);
	}

	private MockUsdAccount account(String accountId) {
		return accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
	}
}
