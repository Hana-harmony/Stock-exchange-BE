package com.hana.exchange.account.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.account.domain.RefreshSession;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.AuthProperties;

@Service
public class RefreshTokenService {

	private static final int REFRESH_TOKEN_BYTES = 48;
	private final RefreshSessionRepository refreshSessionRepository;
	private final IdGenerator idGenerator;
	private final AuthProperties authProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
			RefreshSessionRepository refreshSessionRepository,
			IdGenerator idGenerator,
			AuthProperties authProperties) {
		this.refreshSessionRepository = refreshSessionRepository;
		this.idGenerator = idGenerator;
		this.authProperties = authProperties;
	}

	public IssuedRefreshSession issue(ExchangeUser user, MockUsdAccount account) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(authProperties.refreshTokenTtl());
		String refreshToken = randomRefreshToken();
		RefreshSession session = new RefreshSession(
				idGenerator.newSessionId(),
				user.userId(),
				account.accountId(),
				hash(refreshToken),
				issuedAt,
				expiresAt,
				null,
				null);
		refreshSessionRepository.save(session);
		return new IssuedRefreshSession(refreshToken, session);
	}

	public RefreshSession requireActive(String refreshToken) {
		RefreshSession session = refreshSessionRepository.findByRefreshTokenHash(hash(refreshToken))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		if (!session.activeAt(Instant.now())) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		return session;
	}

	public RefreshSession revoke(RefreshSession session, String replacedBySessionId) {
		RefreshSession revokedSession = session.revoke(Instant.now(), replacedBySessionId);
		refreshSessionRepository.save(revokedSession);
		return revokedSession;
	}

	private String randomRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String refreshToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (Exception exception) {
			throw new IllegalStateException("Refresh token hash failed", exception);
		}
	}

	public record IssuedRefreshSession(
			String refreshToken,
			RefreshSession session
	) {
	}
}
