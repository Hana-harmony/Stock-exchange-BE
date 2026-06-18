package com.hana.exchange.account.application;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockCashLedgerEntry;
import com.hana.exchange.account.domain.MockUsdAccount;

@Repository
public class InMemoryAccountRepository implements AccountRepository {

	private final Map<String, ExchangeUser> usersByUsername = new ConcurrentHashMap<>();
	private final Map<String, MockUsdAccount> accountsById = new ConcurrentHashMap<>();
	private final Map<String, MockCashLedgerEntry> ledgerEntriesById = new ConcurrentHashMap<>();

	@Override
	public boolean existsByUsername(String username) {
		return usersByUsername.containsKey(username);
	}

	@Override
	public synchronized void saveNewAccount(ExchangeUser user, MockUsdAccount account) {
		if (usersByUsername.containsKey(user.username())) {
			throw new IllegalStateException("username already exists");
		}
		usersByUsername.put(user.username(), user);
		accountsById.put(account.accountId(), account);
	}

	@Override
	public Optional<MockUsdAccount> findAccount(String accountId) {
		return Optional.ofNullable(accountsById.get(accountId));
	}

	@Override
	public void saveAccount(MockUsdAccount account) {
		accountsById.put(account.accountId(), account);
	}

	@Override
	public void saveLedgerEntry(MockCashLedgerEntry ledgerEntry) {
		ledgerEntriesById.put(ledgerEntry.ledgerEntryId(), ledgerEntry);
	}
}
