package com.hana.exchange.account.application;

import java.util.Optional;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockCashLedgerEntry;
import com.hana.exchange.account.domain.MockUsdAccount;

public interface AccountRepository {

	boolean existsByUsername(String username);

	void saveNewAccount(ExchangeUser user, MockUsdAccount account);

	Optional<MockUsdAccount> findAccount(String accountId);

	void saveAccount(MockUsdAccount account);

	void saveLedgerEntry(MockCashLedgerEntry ledgerEntry);
}
