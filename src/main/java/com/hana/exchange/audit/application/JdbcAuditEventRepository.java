package com.hana.exchange.audit.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hana.exchange.audit.domain.AuditEvent;
import com.hana.exchange.audit.domain.AuditEventType;

@Repository
@Profile("!memory")
public class JdbcAuditEventRepository implements AuditEventRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcAuditEventRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void save(AuditEvent event) {
		jdbcTemplate.update(
				"INSERT INTO audit_events "
						+ "(audit_event_id, account_id, user_id, event_type, subject_type, subject_id, summary, occurred_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				event.auditEventId(),
				event.accountId(),
				event.userId(),
				event.eventType().name(),
				event.subjectType(),
				event.subjectId(),
				event.summary(),
				timestamp(event.occurredAt()));
	}

	@Override
	public List<AuditEvent> findByAccountId(String accountId, int limit) {
		return jdbcTemplate.query(
				"SELECT audit_event_id, account_id, user_id, event_type, subject_type, subject_id, summary, occurred_at "
						+ "FROM audit_events WHERE account_id = ? ORDER BY occurred_at DESC LIMIT ?",
				(resultSet, rowNumber) -> event(resultSet),
				accountId,
				limit);
	}

	@Override
	public int deleteOccurredBefore(Instant cutoff) {
		return jdbcTemplate.update("DELETE FROM audit_events WHERE occurred_at < ?", timestamp(cutoff));
	}

	private AuditEvent event(ResultSet resultSet) throws SQLException {
		return new AuditEvent(
				resultSet.getString("audit_event_id"),
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				AuditEventType.valueOf(resultSet.getString("event_type")),
				resultSet.getString("subject_type"),
				resultSet.getString("subject_id"),
				resultSet.getString("summary"),
				resultSet.getTimestamp("occurred_at").toInstant());
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}
}
