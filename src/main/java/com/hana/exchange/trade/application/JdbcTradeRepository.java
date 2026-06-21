package com.hana.exchange.trade.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.PendingLimitOrder;
import com.hana.exchange.trade.domain.PortfolioValuationSnapshot;
import com.hana.exchange.trade.domain.TradeOrderStatus;
import com.hana.exchange.trade.domain.TradeSide;

@Repository
@Profile("!memory")
public class JdbcTradeRepository implements TradeRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcTradeRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<MockHolding> findHolding(String accountId, String stockCode) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id, stock_code, stock_name, quantity, average_price_usd, "
						+ "cost_basis_usd, created_at, updated_at "
						+ "FROM mock_holdings WHERE account_id = ? AND stock_code = ?",
				(resultSet, rowNumber) -> holding(resultSet),
				accountId,
				stockCode)
				.stream()
				.findFirst();
	}

	@Override
	public List<MockHolding> findHoldings(String accountId) {
		return jdbcTemplate.query(
				"SELECT account_id, user_id, stock_code, stock_name, quantity, average_price_usd, "
						+ "cost_basis_usd, created_at, updated_at "
						+ "FROM mock_holdings WHERE account_id = ? ORDER BY stock_code ASC",
				(resultSet, rowNumber) -> holding(resultSet),
				accountId);
	}

	@Override
	public List<MockHolding> findHoldingsByStockCodes(List<String> stockCodes) {
		if (stockCodes.isEmpty()) {
			return List.of();
		}
		String placeholders = String.join(",", stockCodes.stream().map(ignored -> "?").toList());
		return jdbcTemplate.query(
				"SELECT account_id, user_id, stock_code, stock_name, quantity, average_price_usd, "
						+ "cost_basis_usd, created_at, updated_at "
						+ "FROM mock_holdings WHERE stock_code IN (" + placeholders + ") "
						+ "ORDER BY account_id ASC, stock_code ASC",
				(resultSet, rowNumber) -> holding(resultSet),
				stockCodes.toArray());
	}

	@Override
	public void saveHolding(MockHolding holding) {
		if (holding.quantity() == 0) {
			jdbcTemplate.update(
					"DELETE FROM mock_holdings WHERE account_id = ? AND stock_code = ?",
					holding.accountId(),
					holding.stockCode());
			return;
		}
		int updated = jdbcTemplate.update(
				"UPDATE mock_holdings "
						+ "SET stock_name = ?, quantity = ?, average_price_usd = ?, cost_basis_usd = ?, updated_at = ? "
						+ "WHERE account_id = ? AND stock_code = ?",
				holding.stockName(),
				holding.quantity(),
				holding.averagePriceUsd(),
				holding.costBasisUsd(),
				timestamp(holding.updatedAt()),
				holding.accountId(),
				holding.stockCode());
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO mock_holdings "
							+ "(account_id, user_id, stock_code, stock_name, quantity, average_price_usd, "
							+ "cost_basis_usd, created_at, updated_at) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
					holding.accountId(),
					holding.userId(),
					holding.stockCode(),
					holding.stockName(),
					holding.quantity(),
					holding.averagePriceUsd(),
					holding.costBasisUsd(),
					timestamp(holding.createdAt()),
					timestamp(holding.updatedAt()));
		}
	}

	@Override
	public void saveTrade(MockTradeLedgerEntry trade) {
		jdbcTemplate.update(
				"INSERT INTO mock_trade_ledger_entries "
						+ "(trade_id, account_id, user_id, stock_code, stock_name, side, quantity, "
						+ "execution_price_usd, gross_amount_usd, realized_pnl_usd, remaining_quantity, "
						+ "average_price_usd_after, cash_balance_usd_after, executed_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				trade.tradeId(),
				trade.accountId(),
				trade.userId(),
				trade.stockCode(),
				trade.stockName(),
				trade.side().name(),
				trade.quantity(),
				trade.executionPriceUsd(),
				trade.grossAmountUsd(),
				trade.realizedPnlUsd(),
				trade.remainingQuantity(),
				trade.averagePriceUsdAfter(),
				trade.cashBalanceUsdAfter(),
				timestamp(trade.executedAt()));
	}

	@Override
	public List<MockTradeLedgerEntry> findTrades(String accountId) {
		return jdbcTemplate.query(
				"SELECT trade_id, account_id, user_id, stock_code, stock_name, side, quantity, "
						+ "execution_price_usd, gross_amount_usd, realized_pnl_usd, remaining_quantity, "
						+ "average_price_usd_after, cash_balance_usd_after, executed_at "
						+ "FROM mock_trade_ledger_entries WHERE account_id = ? ORDER BY executed_at ASC",
				(resultSet, rowNumber) -> trade(resultSet),
				accountId);
	}

	@Override
	public List<MockTradeLedgerEntry> findRecentTrades(String accountId, int limit) {
		return jdbcTemplate.query(
				"SELECT trade_id, account_id, user_id, stock_code, stock_name, side, quantity, "
						+ "execution_price_usd, gross_amount_usd, realized_pnl_usd, remaining_quantity, "
						+ "average_price_usd_after, cash_balance_usd_after, executed_at "
						+ "FROM mock_trade_ledger_entries WHERE account_id = ? "
						+ "ORDER BY executed_at DESC LIMIT ?",
				(resultSet, rowNumber) -> trade(resultSet),
				accountId,
				limit);
	}

	@Override
	public void saveLimitOrder(PendingLimitOrder order) {
		int updated = jdbcTemplate.update(
				"UPDATE pending_limit_orders "
						+ "SET observed_price_usd = ?, status = ?, trade_id = ?, filled_at = ? "
						+ "WHERE order_id = ?",
				order.observedPriceUsd(),
				order.status().name(),
				order.tradeId(),
				timestamp(order.filledAt()),
				order.orderId());
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO pending_limit_orders "
							+ "(order_id, account_id, user_id, stock_code, stock_name, side, quantity, "
							+ "limit_price_usd, observed_price_usd, status, trade_id, created_at, filled_at) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					order.orderId(),
					order.accountId(),
					order.userId(),
					order.stockCode(),
					order.stockName(),
					order.side().name(),
					order.quantity(),
					order.limitPriceUsd(),
					order.observedPriceUsd(),
					order.status().name(),
					order.tradeId(),
					timestamp(order.createdAt()),
					timestamp(order.filledAt()));
		}
	}

	@Override
	public List<PendingLimitOrder> findRecentLimitOrders(String accountId, int limit) {
		return jdbcTemplate.query(
				"SELECT order_id, account_id, user_id, stock_code, stock_name, side, quantity, "
						+ "limit_price_usd, observed_price_usd, status, trade_id, created_at, filled_at "
						+ "FROM pending_limit_orders WHERE account_id = ? "
						+ "ORDER BY created_at DESC LIMIT ?",
				(resultSet, rowNumber) -> limitOrder(resultSet),
				accountId,
				limit);
	}

	@Override
	public List<PendingLimitOrder> findPendingLimitOrdersByStockCode(String stockCode) {
		return jdbcTemplate.query(
				"SELECT order_id, account_id, user_id, stock_code, stock_name, side, quantity, "
						+ "limit_price_usd, observed_price_usd, status, trade_id, created_at, filled_at "
						+ "FROM pending_limit_orders WHERE stock_code = ? AND status = ? "
						+ "ORDER BY created_at ASC",
				(resultSet, rowNumber) -> limitOrder(resultSet),
				stockCode,
				TradeOrderStatus.PENDING.name());
	}

	@Override
	public void savePortfolioValuationSnapshot(PortfolioValuationSnapshot snapshot) {
		jdbcTemplate.update(
				"INSERT INTO portfolio_valuation_snapshots "
						+ "(snapshot_id, account_id, user_id, currency, cash_balance_usd, total_market_value_usd, "
						+ "total_asset_value_usd, realized_pnl_usd, unrealized_pnl_usd, holding_count, valued_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				snapshot.snapshotId(),
				snapshot.accountId(),
				snapshot.userId(),
				snapshot.currency(),
				snapshot.cashBalanceUsd(),
				snapshot.totalMarketValueUsd(),
				snapshot.totalAssetValueUsd(),
				snapshot.realizedPnlUsd(),
				snapshot.unrealizedPnlUsd(),
				snapshot.holdingCount(),
				timestamp(snapshot.valuedAt()));
	}

	@Override
	public List<PortfolioValuationSnapshot> findPortfolioValuationSnapshots(String accountId, int limit) {
		return jdbcTemplate.query(
				"SELECT snapshot_id, account_id, user_id, currency, cash_balance_usd, total_market_value_usd, "
						+ "total_asset_value_usd, realized_pnl_usd, unrealized_pnl_usd, holding_count, valued_at "
						+ "FROM portfolio_valuation_snapshots WHERE account_id = ? "
						+ "ORDER BY valued_at DESC LIMIT ?",
				(resultSet, rowNumber) -> portfolioValuationSnapshot(resultSet),
				accountId,
				limit);
	}

	private MockHolding holding(ResultSet resultSet) throws SQLException {
		return new MockHolding(
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("stock_code"),
				resultSet.getString("stock_name"),
				resultSet.getLong("quantity"),
				resultSet.getBigDecimal("average_price_usd"),
				resultSet.getBigDecimal("cost_basis_usd"),
				instant(resultSet, "created_at"),
				instant(resultSet, "updated_at"));
	}

	private MockTradeLedgerEntry trade(ResultSet resultSet) throws SQLException {
		return new MockTradeLedgerEntry(
				resultSet.getString("trade_id"),
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("stock_code"),
				resultSet.getString("stock_name"),
				TradeSide.valueOf(resultSet.getString("side")),
				resultSet.getLong("quantity"),
				resultSet.getBigDecimal("execution_price_usd"),
				resultSet.getBigDecimal("gross_amount_usd"),
				resultSet.getBigDecimal("realized_pnl_usd"),
				resultSet.getLong("remaining_quantity"),
				resultSet.getBigDecimal("average_price_usd_after"),
				resultSet.getBigDecimal("cash_balance_usd_after"),
				instant(resultSet, "executed_at"));
	}

	private PendingLimitOrder limitOrder(ResultSet resultSet) throws SQLException {
		return new PendingLimitOrder(
				resultSet.getString("order_id"),
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("stock_code"),
				resultSet.getString("stock_name"),
				TradeSide.valueOf(resultSet.getString("side")),
				resultSet.getLong("quantity"),
				resultSet.getBigDecimal("limit_price_usd"),
				resultSet.getBigDecimal("observed_price_usd"),
				TradeOrderStatus.valueOf(resultSet.getString("status")),
				resultSet.getString("trade_id"),
				instant(resultSet, "created_at"),
				nullableInstant(resultSet, "filled_at"));
	}

	private PortfolioValuationSnapshot portfolioValuationSnapshot(ResultSet resultSet) throws SQLException {
		return new PortfolioValuationSnapshot(
				resultSet.getString("snapshot_id"),
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("currency"),
				resultSet.getBigDecimal("cash_balance_usd"),
				resultSet.getBigDecimal("total_market_value_usd"),
				resultSet.getBigDecimal("total_asset_value_usd"),
				resultSet.getBigDecimal("realized_pnl_usd"),
				resultSet.getBigDecimal("unrealized_pnl_usd"),
				resultSet.getInt("holding_count"),
				instant(resultSet, "valued_at"));
	}

	private Timestamp timestamp(Instant instant) {
		if (instant == null) {
			return null;
		}
		return Timestamp.from(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}

	private Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(column);
		return timestamp == null ? null : timestamp.toInstant();
	}
}
