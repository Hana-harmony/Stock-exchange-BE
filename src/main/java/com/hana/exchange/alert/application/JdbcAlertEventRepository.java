package com.hana.exchange.alert.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventMatchResult;
import com.hana.exchange.alert.domain.AlertGlossaryTerm;
import com.hana.exchange.alert.domain.AlertTargetResponse;

@Repository
@Profile("!memory")
public class JdbcAlertEventRepository implements AlertEventRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcAlertEventRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<AlertEventMatchResult> findByIdempotencyKey(String idempotencyKey) {
		return jdbcTemplate.query(
				eventSelect() + " WHERE idempotency_key = ?",
				(resultSet, rowNumber) -> result(resultSet),
				idempotencyKey)
				.stream()
				.findFirst();
	}

	@Override
	public Optional<AlertEventMatchResult> findByEventId(String eventId) {
		return jdbcTemplate.query(
				eventSelect() + " WHERE event_id = ?",
				(resultSet, rowNumber) -> result(resultSet),
				eventId)
				.stream()
				.findFirst();
	}

	@Override
	public List<AlertEventMatchResult> findByStockCode(String stockCode) {
		return jdbcTemplate.query(
				eventSelect()
						+ " WHERE stock_code = ? "
						+ "OR EXISTS (SELECT 1 FROM alert_event_related_stocks related "
						+ "WHERE related.event_id = alert_events.event_id AND related.stock_code = ?) "
						+ "ORDER BY published_at DESC",
				(resultSet, rowNumber) -> result(resultSet),
				stockCode,
				stockCode);
	}

	@Override
	@Transactional
	public void save(AlertEvent event, AlertEventMatchResult matchResult) {
		jdbcTemplate.update(
				"INSERT INTO alert_events "
						+ "(event_id, idempotency_key, source_type, title, summary, original_url, stock_code, "
						+ "sentiment, importance, risk_level, watchlist_target, holder_target, "
						+ "published_at, received_at, matched_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				event.eventId(),
				event.idempotencyKey(),
				event.sourceType(),
				event.title(),
				event.summary(),
				event.originalUrl(),
				event.stockCode(),
				event.sentiment(),
				event.importance(),
				event.riskLevel(),
				event.watchlistTarget(),
				event.holderTarget(),
				timestamp(event.publishedAt()),
				timestamp(event.receivedAt()),
				timestamp(matchResult.matchedAt()));
		insertRelatedStocks(event);
		insertGlossaryTerms(event);
		insertTranslationQualityFlags(event);
		insertTargets(event.eventId(), matchResult.targets());
	}

	private void insertRelatedStocks(AlertEvent event) {
		for (int index = 0; index < event.relatedStocks().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO alert_event_related_stocks (event_id, stock_code, sort_order) VALUES (?, ?, ?)",
					event.eventId(),
					event.relatedStocks().get(index),
					index);
		}
	}

	private void insertGlossaryTerms(AlertEvent event) {
		for (int index = 0; index < event.glossaryTerms().size(); index++) {
			AlertGlossaryTerm term = event.glossaryTerms().get(index);
			jdbcTemplate.update(
					"INSERT INTO alert_event_glossary_terms "
							+ "(event_id, source_term, normalized_term, english_term, category, sort_order) "
							+ "VALUES (?, ?, ?, ?, ?, ?)",
					event.eventId(),
					term.sourceTerm(),
					term.normalizedTerm(),
					term.englishTerm(),
					term.category(),
					index);
		}
	}

	private void insertTranslationQualityFlags(AlertEvent event) {
		for (int index = 0; index < event.translationQualityFlags().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO alert_event_translation_quality_flags "
							+ "(event_id, quality_flag, sort_order) VALUES (?, ?, ?)",
					event.eventId(),
					event.translationQualityFlags().get(index),
					index);
		}
	}

	private void insertTargets(String eventId, List<AlertTargetResponse> targets) {
		for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
			AlertTargetResponse target = targets.get(targetIndex);
			jdbcTemplate.update(
					"INSERT INTO alert_event_targets (event_id, account_id, user_id, sort_order) "
							+ "VALUES (?, ?, ?, ?)",
					eventId,
					target.accountId(),
					target.userId(),
					targetIndex);
			insertTargetReasons(eventId, target);
			insertTargetStocks(eventId, target);
		}
	}

	private void insertTargetReasons(String eventId, AlertTargetResponse target) {
		for (int index = 0; index < target.matchReasons().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO alert_event_target_reasons "
							+ "(event_id, account_id, match_reason, sort_order) VALUES (?, ?, ?, ?)",
					eventId,
					target.accountId(),
					target.matchReasons().get(index),
					index);
		}
	}

	private void insertTargetStocks(String eventId, AlertTargetResponse target) {
		for (int index = 0; index < target.matchedStockCodes().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO alert_event_target_stocks "
							+ "(event_id, account_id, stock_code, sort_order) VALUES (?, ?, ?, ?)",
					eventId,
					target.accountId(),
					target.matchedStockCodes().get(index),
					index);
		}
	}

	private AlertEventMatchResult result(ResultSet resultSet) throws SQLException {
		AlertEvent event = new AlertEvent(
				resultSet.getString("event_id"),
				resultSet.getString("idempotency_key"),
				resultSet.getString("source_type"),
				resultSet.getString("title"),
				resultSet.getString("summary"),
				resultSet.getString("original_url"),
				resultSet.getString("stock_code"),
				relatedStocks(resultSet.getString("event_id")),
				glossaryTerms(resultSet.getString("event_id")),
				translationQualityFlags(resultSet.getString("event_id")),
				resultSet.getString("sentiment"),
				resultSet.getString("importance"),
				resultSet.getString("risk_level"),
				resultSet.getBoolean("watchlist_target"),
				resultSet.getBoolean("holder_target"),
				instant(resultSet, "published_at"),
				instant(resultSet, "received_at"));
		return new AlertEventMatchResult(
				event,
				targets(event.eventId()),
				instant(resultSet, "matched_at"));
	}

	private List<String> relatedStocks(String eventId) {
		return jdbcTemplate.query(
				"SELECT stock_code FROM alert_event_related_stocks "
						+ "WHERE event_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("stock_code"),
				eventId);
	}

	private List<AlertGlossaryTerm> glossaryTerms(String eventId) {
		return jdbcTemplate.query(
				"SELECT source_term, normalized_term, english_term, category FROM alert_event_glossary_terms "
						+ "WHERE event_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> new AlertGlossaryTerm(
						resultSet.getString("source_term"),
						resultSet.getString("normalized_term"),
						resultSet.getString("english_term"),
						resultSet.getString("category")),
				eventId);
	}

	private List<String> translationQualityFlags(String eventId) {
		return jdbcTemplate.query(
				"SELECT quality_flag FROM alert_event_translation_quality_flags "
						+ "WHERE event_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("quality_flag"),
				eventId);
	}

	private List<AlertTargetResponse> targets(String eventId) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id FROM alert_event_targets "
						+ "WHERE event_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> new AlertTargetResponse(
						resultSet.getString("account_id"),
						resultSet.getString("user_id"),
						targetReasons(eventId, resultSet.getString("account_id")),
						targetStocks(eventId, resultSet.getString("account_id"))),
				eventId);
	}

	private List<String> targetReasons(String eventId, String accountId) {
		return jdbcTemplate.query(
				"SELECT match_reason FROM alert_event_target_reasons "
						+ "WHERE event_id = ? AND account_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("match_reason"),
				eventId,
				accountId);
	}

	private List<String> targetStocks(String eventId, String accountId) {
		return jdbcTemplate.query(
				"SELECT stock_code FROM alert_event_target_stocks "
						+ "WHERE event_id = ? AND account_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("stock_code"),
				eventId,
				accountId);
	}

	private String eventSelect() {
		return "SELECT event_id, idempotency_key, source_type, title, summary, original_url, stock_code, "
				+ "sentiment, importance, risk_level, watchlist_target, holder_target, "
				+ "published_at, received_at, matched_at FROM alert_events";
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}
}
