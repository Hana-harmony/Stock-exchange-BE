package com.hana.exchange.account.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hana.exchange.account.domain.RefreshSession;

@Repository
@Profile("!memory")
public class JdbcRefreshSessionRepository implements RefreshSessionRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcRefreshSessionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void save(RefreshSession refreshSession) {
		int updated = jdbcTemplate.update(
				"UPDATE refresh_sessions "
						+ "SET revoked_at = ?, replaced_by_session_id = ? "
						+ "WHERE session_id = ?",
				nullableTimestamp(refreshSession.revokedAt()),
				refreshSession.replacedBySessionId(),
				refreshSession.sessionId());
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO refresh_sessions "
							+ "(session_id, user_id, account_id, refresh_token_hash, issued_at, "
							+ "expires_at, revoked_at, replaced_by_session_id) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					refreshSession.sessionId(),
					refreshSession.userId(),
					refreshSession.accountId(),
					refreshSession.refreshTokenHash(),
					timestamp(refreshSession.issuedAt()),
					timestamp(refreshSession.expiresAt()),
					nullableTimestamp(refreshSession.revokedAt()),
					refreshSession.replacedBySessionId());
		}
	}

	@Override
	public Optional<RefreshSession> findByRefreshTokenHash(String refreshTokenHash) {
		return jdbcTemplate.query(
				"SELECT session_id, user_id, account_id, refresh_token_hash, issued_at, expires_at, "
						+ "revoked_at, replaced_by_session_id "
						+ "FROM refresh_sessions WHERE refresh_token_hash = ?",
				(resultSet, rowNumber) -> refreshSession(resultSet),
				refreshTokenHash)
				.stream()
				.findFirst();
	}

	private RefreshSession refreshSession(ResultSet resultSet) throws SQLException {
		return new RefreshSession(
				resultSet.getString("session_id"),
				resultSet.getString("user_id"),
				resultSet.getString("account_id"),
				resultSet.getString("refresh_token_hash"),
				instant(resultSet, "issued_at"),
				instant(resultSet, "expires_at"),
				nullableInstant(resultSet, "revoked_at"),
				resultSet.getString("replaced_by_session_id"));
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private Timestamp nullableTimestamp(Instant instant) {
		return instant == null ? null : timestamp(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}

	private Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(column);
		return timestamp == null ? null : timestamp.toInstant();
	}
}
