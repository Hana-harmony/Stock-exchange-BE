package com.hana.exchange.account.application;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.account.domain.RefreshSession;

@Repository
@Profile("memory")
public class InMemoryRefreshSessionRepository implements RefreshSessionRepository {

	private final Map<String, RefreshSession> sessionsByRefreshTokenHash = new ConcurrentHashMap<>();

	@Override
	public void save(RefreshSession refreshSession) {
		sessionsByRefreshTokenHash.put(refreshSession.refreshTokenHash(), refreshSession);
	}

	@Override
	public Optional<RefreshSession> findByRefreshTokenHash(String refreshTokenHash) {
		return Optional.ofNullable(sessionsByRefreshTokenHash.get(refreshTokenHash));
	}
}
