package com.hana.exchange.account.application;

import java.util.Optional;

import com.hana.exchange.account.domain.RefreshSession;

public interface RefreshSessionRepository {

	void save(RefreshSession refreshSession);

	Optional<RefreshSession> findByRefreshTokenHash(String refreshTokenHash);
}
