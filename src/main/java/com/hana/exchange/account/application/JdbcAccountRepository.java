package com.hana.exchange.account.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockCashLedgerEntry;
import com.hana.exchange.account.domain.MockUsdAccount;

@Repository
@Profile("!memory")
public class JdbcAccountRepository implements AccountRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcAccountRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean existsByUsername(String username) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM exchange_users WHERE username = ?",
				Integer.class,
				username);
		return count != null && count > 0;
	}

	@Override
	public Optional<ExchangeUser> findUserByUsername(String username) {
		return jdbcTemplate.query(
				"SELECT user_id, username, password_salt, password_hash, created_at "
						+ "FROM exchange_users WHERE username = ?",
				(resultSet, rowNumber) -> user(resultSet),
				username)
				.stream()
				.findFirst();
	}

	@Override
	public Optional<ExchangeUser> findUserById(String userId) {
		return jdbcTemplate.query(
				"SELECT user_id, username, password_salt, password_hash, created_at "
						+ "FROM exchange_users WHERE user_id = ?",
				(resultSet, rowNumber) -> user(resultSet),
				userId)
				.stream()
				.findFirst();
	}

	@Override
	public Optional<MockUsdAccount> findAccountByUserId(String userId) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id, currency, cash_balance_usd, created_at, updated_at "
						+ "FROM mock_usd_accounts WHERE user_id = ?",
				(resultSet, rowNumber) -> account(resultSet),
				userId)
				.stream()
				.findFirst();
	}

	@Override
	@Transactional
	public void saveNewAccount(ExchangeUser user, MockUsdAccount account) {
		try {
			jdbcTemplate.update(
					"INSERT INTO exchange_users "
							+ "(user_id, username, password_salt, password_hash, created_at) "
							+ "VALUES (?, ?, ?, ?, ?)",
					user.userId(),
					user.username(),
					user.passwordSalt(),
					user.passwordHash(),
					timestamp(user.createdAt()));
			jdbcTemplate.update(
					"INSERT INTO mock_usd_accounts "
							+ "(account_id, user_id, currency, cash_balance_usd, created_at, updated_at) "
							+ "VALUES (?, ?, ?, ?, ?, ?)",
					account.accountId(),
					account.userId(),
					account.currency(),
					account.cashBalanceUsd(),
					timestamp(account.createdAt()),
					timestamp(account.updatedAt()));
		} catch (DuplicateKeyException exception) {
			throw new IllegalStateException("username already exists", exception);
		}
	}

	@Override
	public Optional<MockUsdAccount> findAccount(String accountId) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id, currency, cash_balance_usd, created_at, updated_at "
						+ "FROM mock_usd_accounts WHERE account_id = ?",
				(resultSet, rowNumber) -> account(resultSet),
				accountId)
				.stream()
				.findFirst();
	}

	@Override
	public void saveAccount(MockUsdAccount account) {
		int updated = jdbcTemplate.update(
				"UPDATE mock_usd_accounts "
						+ "SET cash_balance_usd = ?, updated_at = ? "
						+ "WHERE account_id = ?",
				account.cashBalanceUsd(),
				timestamp(account.updatedAt()),
				account.accountId());
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO mock_usd_accounts "
							+ "(account_id, user_id, currency, cash_balance_usd, created_at, updated_at) "
							+ "VALUES (?, ?, ?, ?, ?, ?)",
					account.accountId(),
					account.userId(),
					account.currency(),
					account.cashBalanceUsd(),
					timestamp(account.createdAt()),
					timestamp(account.updatedAt()));
		}
	}

	@Override
	public void saveLedgerEntry(MockCashLedgerEntry ledgerEntry) {
		jdbcTemplate.update(
				"INSERT INTO mock_cash_ledger_entries "
						+ "(ledger_entry_id, account_id, ledger_type, amount_usd, balance_after_usd, created_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?)",
				ledgerEntry.ledgerEntryId(),
				ledgerEntry.accountId(),
				ledgerEntry.type(),
				ledgerEntry.amountUsd(),
				ledgerEntry.balanceAfterUsd(),
				timestamp(ledgerEntry.createdAt()));
	}

	private ExchangeUser user(ResultSet resultSet) throws SQLException {
		return new ExchangeUser(
				resultSet.getString("user_id"),
				resultSet.getString("username"),
				resultSet.getString("password_salt"),
				resultSet.getString("password_hash"),
				instant(resultSet, "created_at"));
	}

	private MockUsdAccount account(ResultSet resultSet) throws SQLException {
		return new MockUsdAccount(
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("currency"),
				resultSet.getBigDecimal("cash_balance_usd"),
				instant(resultSet, "created_at"),
				instant(resultSet, "updated_at"));
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}
}
