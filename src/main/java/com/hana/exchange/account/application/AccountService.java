package com.hana.exchange.account.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.domain.AccountBalanceResponse;
import com.hana.exchange.account.domain.DepositUsdRequest;
import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockCashLedgerEntry;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.account.domain.PasswordHash;
import com.hana.exchange.account.domain.SignUpRequest;
import com.hana.exchange.account.domain.SignUpResponse;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;

@Service
public class AccountService {

	private static final String USD = "USD";
	private static final String TRADING_MODE = "EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING";

	private final AccountRepository accountRepository;
	private final IdGenerator idGenerator;
	private final PasswordHasher passwordHasher;

	public AccountService(
			AccountRepository accountRepository,
			IdGenerator idGenerator,
			PasswordHasher passwordHasher) {
		this.accountRepository = accountRepository;
		this.idGenerator = idGenerator;
		this.passwordHasher = passwordHasher;
	}

	public SignUpResponse signUp(SignUpRequest request) {
		String normalizedUsername = request.username().trim().toLowerCase();
		if (accountRepository.existsByUsername(normalizedUsername)) {
			throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
		}

		Instant now = Instant.now();
		PasswordHash passwordHash = passwordHasher.hash(request.password());
		ExchangeUser user = new ExchangeUser(
				idGenerator.newUserId(),
				normalizedUsername,
				passwordHash.salt(),
				passwordHash.hash(),
				now);
		MockUsdAccount account = new MockUsdAccount(
				idGenerator.newAccountId(),
				user.userId(),
				USD,
				BigDecimal.ZERO.setScale(2),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		return toSignUpResponse(user, account);
	}

	public AccountBalanceResponse getAccount(String accountId) {
		MockUsdAccount account = account(accountId);
		return toAccountBalanceResponse(account, null);
	}

	public AccountBalanceResponse depositUsd(String accountId, DepositUsdRequest request) {
		MockUsdAccount account = account(accountId);
		BigDecimal amount = money(request.amountUsd());
		Instant now = Instant.now();
		MockUsdAccount updatedAccount = account.deposit(amount, now);
		MockCashLedgerEntry ledgerEntry = new MockCashLedgerEntry(
				idGenerator.newLedgerEntryId(),
				updatedAccount.accountId(),
				"MOCK_USD_DEPOSIT",
				amount,
				updatedAccount.cashBalanceUsd(),
				now);
		accountRepository.saveAccount(updatedAccount);
		accountRepository.saveLedgerEntry(ledgerEntry);
		return toAccountBalanceResponse(updatedAccount, ledgerEntry.ledgerEntryId());
	}

	private MockUsdAccount account(String accountId) {
		return accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
	}

	private SignUpResponse toSignUpResponse(ExchangeUser user, MockUsdAccount account) {
		return new SignUpResponse(
				user.userId(),
				user.username(),
				account.accountId(),
				account.currency(),
				moneyText(account.cashBalanceUsd()),
				TRADING_MODE,
				account.createdAt());
	}

	private AccountBalanceResponse toAccountBalanceResponse(MockUsdAccount account, String lastLedgerEntryId) {
		return new AccountBalanceResponse(
				account.userId(),
				account.accountId(),
				account.currency(),
				moneyText(account.cashBalanceUsd()),
				lastLedgerEntryId,
				account.updatedAt());
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.UNNECESSARY);
	}

	private String moneyText(BigDecimal value) {
		return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
	}
}
