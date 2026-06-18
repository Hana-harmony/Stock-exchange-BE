package com.hana.exchange.watchlist.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hana.exchange.watchlist.domain.WatchlistItem;

@Repository
@Profile("!memory")
public class JdbcWatchlistRepository implements WatchlistRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcWatchlistRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<WatchlistItem> findItems(String accountId) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id, stock_code, stock_name, market, targeting_mode, added_at "
						+ "FROM watchlist_items WHERE account_id = ? ORDER BY added_at ASC",
				(resultSet, rowNumber) -> item(resultSet),
				accountId);
	}

	@Override
	public List<WatchlistItem> findItemsByStockCodes(List<String> stockCodes) {
		if (stockCodes.isEmpty()) {
			return List.of();
		}
		String placeholders = String.join(",", stockCodes.stream().map(ignored -> "?").toList());
		return jdbcTemplate.query(
				"SELECT account_id, user_id, stock_code, stock_name, market, targeting_mode, added_at "
						+ "FROM watchlist_items WHERE stock_code IN (" + placeholders + ") "
						+ "ORDER BY account_id ASC, stock_code ASC",
				(resultSet, rowNumber) -> item(resultSet),
				stockCodes.toArray());
	}

	@Override
	public Optional<WatchlistItem> findItem(String accountId, String stockCode) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id, stock_code, stock_name, market, targeting_mode, added_at "
						+ "FROM watchlist_items WHERE account_id = ? AND stock_code = ?",
				(resultSet, rowNumber) -> item(resultSet),
				accountId,
				stockCode)
				.stream()
				.findFirst();
	}

	@Override
	public void saveItem(WatchlistItem item) {
		int updated = jdbcTemplate.update(
				"UPDATE watchlist_items "
						+ "SET stock_name = ?, market = ?, targeting_mode = ? "
						+ "WHERE account_id = ? AND stock_code = ?",
				item.stockName(),
				item.market(),
				item.targetingMode(),
				item.accountId(),
				item.stockCode());
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO watchlist_items "
							+ "(account_id, user_id, stock_code, stock_name, market, targeting_mode, added_at) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?)",
					item.accountId(),
					item.userId(),
					item.stockCode(),
					item.stockName(),
					item.market(),
					item.targetingMode(),
					timestamp(item.addedAt()));
		}
	}

	@Override
	public boolean deleteItem(String accountId, String stockCode) {
		int deleted = jdbcTemplate.update(
				"DELETE FROM watchlist_items WHERE account_id = ? AND stock_code = ?",
				accountId,
				stockCode);
		return deleted > 0;
	}

	private WatchlistItem item(ResultSet resultSet) throws SQLException {
		return new WatchlistItem(
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("stock_code"),
				resultSet.getString("stock_name"),
				resultSet.getString("market"),
				resultSet.getString("targeting_mode"),
				instant(resultSet, "added_at"));
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}
}
