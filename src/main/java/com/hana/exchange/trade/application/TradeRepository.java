package com.hana.exchange.trade.application;

import java.util.List;
import java.util.Optional;

import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.PortfolioValuationSnapshot;

public interface TradeRepository {

	Optional<MockHolding> findHolding(String accountId, String stockCode);

	List<MockHolding> findHoldings(String accountId);

	List<MockHolding> findHoldingsByStockCodes(List<String> stockCodes);

	void saveHolding(MockHolding holding);

	void saveTrade(MockTradeLedgerEntry trade);

	List<MockTradeLedgerEntry> findTrades(String accountId);

	List<MockTradeLedgerEntry> findRecentTrades(String accountId, int limit);

	void savePortfolioValuationSnapshot(PortfolioValuationSnapshot snapshot);

	List<PortfolioValuationSnapshot> findPortfolioValuationSnapshots(String accountId, int limit);
}
