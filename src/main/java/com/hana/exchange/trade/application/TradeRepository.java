package com.hana.exchange.trade.application;

import java.util.List;
import java.util.Optional;

import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;

public interface TradeRepository {

	Optional<MockHolding> findHolding(String accountId, String stockCode);

	List<MockHolding> findHoldings(String accountId);

	void saveHolding(MockHolding holding);

	void saveTrade(MockTradeLedgerEntry trade);

	List<MockTradeLedgerEntry> findTrades(String accountId);

	List<MockTradeLedgerEntry> findRecentTrades(String accountId, int limit);
}
