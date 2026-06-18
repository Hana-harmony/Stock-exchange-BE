package com.hana.exchange.account.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.domain.AccountBalanceResponse;
import com.hana.exchange.account.domain.DepositUsdRequest;
import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.LogoutRequest;
import com.hana.exchange.account.domain.LogoutResponse;
import com.hana.exchange.account.domain.LoginRequest;
import com.hana.exchange.account.domain.LoginResponse;
import com.hana.exchange.account.domain.MockCashLedgerEntry;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.account.domain.PasswordHash;
import com.hana.exchange.account.domain.RefreshSession;
import com.hana.exchange.account.domain.RefreshTokenRequest;
import com.hana.exchange.account.domain.RefreshTokenResponse;
import com.hana.exchange.account.domain.SignUpRequest;
import com.hana.exchange.account.domain.SignUpResponse;
import com.hana.exchange.account.domain.TokenVerifyRequest;
import com.hana.exchange.account.domain.TokenVerifyResponse;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;

@Service
public class AccountService {

	private static final String USD = "USD";
	private static final String TRADING_MODE = "EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING";

	private final AccountRepository accountRepository;
	private final IdGenerator idGenerator;
	private final PasswordHasher passwordHasher;
	private final AuthTokenService authTokenService;
	private final RefreshTokenService refreshTokenService;

	public AccountService(
			AccountRepository accountRepository,
			IdGenerator idGenerator,
			PasswordHasher passwordHasher,
			AuthTokenService authTokenService,
			RefreshTokenService refreshTokenService) {
		this.accountRepository = accountRepository;
		this.idGenerator = idGenerator;
		this.passwordHasher = passwordHasher;
		this.authTokenService = authTokenService;
		this.refreshTokenService = refreshTokenService;
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

	public LoginResponse login(LoginRequest request) {
		String normalizedUsername = request.username().trim().toLowerCase();
		ExchangeUser user = accountRepository.findUserByUsername(normalizedUsername)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));
		if (!passwordHasher.matches(request.password(), user.passwordSalt(), user.passwordHash())) {
			throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
		}
		MockUsdAccount account = accountRepository.findAccountByUserId(user.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
		AuthTokenService.IssuedToken token = authTokenService.issue(user, account);
		RefreshTokenService.IssuedRefreshSession refresh = refreshTokenService.issue(user, account);
		return new LoginResponse(
				user.userId(),
				user.username(),
				account.accountId(),
				token.tokenType(),
				token.accessToken(),
				refresh.refreshToken(),
				refresh.session().sessionId(),
				token.issuedAt(),
				token.expiresAt(),
				refresh.session().expiresAt());
	}

	public TokenVerifyResponse verifyToken(TokenVerifyRequest request) {
		AuthTokenService.VerifiedToken token = authTokenService.verify(request.accessToken());
		return new TokenVerifyResponse(
				true,
				token.userId(),
				token.username(),
				token.accountId(),
				token.issuedAt(),
				token.expiresAt());
	}

	public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
		RefreshSession oldSession = refreshTokenService.requireActive(request.refreshToken());
		ExchangeUser user = accountRepository.findUserById(oldSession.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		MockUsdAccount account = accountRepository.findAccount(oldSession.accountId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		AuthTokenService.IssuedToken accessToken = authTokenService.issue(user, account);
		RefreshTokenService.IssuedRefreshSession refresh = refreshTokenService.issue(user, account);
		refreshTokenService.revoke(oldSession, refresh.session().sessionId());
		return new RefreshTokenResponse(
				user.userId(),
				user.username(),
				account.accountId(),
				accessToken.tokenType(),
				accessToken.accessToken(),
				refresh.refreshToken(),
				refresh.session().sessionId(),
				accessToken.issuedAt(),
				accessToken.expiresAt(),
				refresh.session().expiresAt());
	}

	public LogoutResponse logout(LogoutRequest request) {
		RefreshSession session = refreshTokenService.requireActive(request.refreshToken());
		RefreshSession revokedSession = refreshTokenService.revoke(session, null);
		return new LogoutResponse(true, revokedSession.sessionId(), revokedSession.revokedAt());
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
