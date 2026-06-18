package com.hana.exchange.tax.application;

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

import com.hana.exchange.tax.domain.TaxRefundCase;
import com.hana.exchange.tax.domain.TaxRefundCaseStatus;

@Repository
@Profile("!memory")
public class JdbcTaxRefundCaseRepository implements TaxRefundCaseRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcTaxRefundCaseRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<TaxRefundCase> findByAccountIdAndTaxYear(String accountId, int taxYear) {
		return jdbcTemplate.query(
				caseSelect() + " WHERE account_id = ? AND tax_year = ?",
				(resultSet, rowNumber) -> taxCase(resultSet),
				accountId,
				taxYear)
				.stream()
				.findFirst();
	}

	@Override
	public Optional<TaxRefundCase> findLatestByAccountId(String accountId) {
		return jdbcTemplate.query(
				caseSelect() + " WHERE account_id = ? ORDER BY tax_year DESC, updated_at DESC LIMIT 1",
				(resultSet, rowNumber) -> taxCase(resultSet),
				accountId)
				.stream()
				.findFirst();
	}

	@Override
	@Transactional
	public void save(TaxRefundCase taxCase) {
		int updated = jdbcTemplate.update(
				"UPDATE tax_refund_cases "
						+ "SET treaty_country = ?, residence_certificate_file_name = ?, "
						+ "reduced_tax_application_file_name = ?, residence_certificate_document_id = ?, "
						+ "reduced_tax_application_document_id = ?, advance_payment_requested = ?, status = ?, "
						+ "total_sell_amount_usd = ?, realized_profit_usd = ?, realized_loss_usd = ?, "
						+ "net_realized_pnl_usd = ?, taxable_realized_pnl_usd = ?, estimated_withholding_tax_usd = ?, "
						+ "estimated_treaty_tax_usd = ?, estimated_refund_usd = ?, advance_payment_eligible = ?, "
						+ "updated_at = ? WHERE case_id = ?",
				taxCase.treatyCountry(),
				taxCase.residenceCertificateFileName(),
				taxCase.reducedTaxApplicationFileName(),
				taxCase.residenceCertificateDocumentId(),
				taxCase.reducedTaxApplicationDocumentId(),
				taxCase.advancePaymentRequested(),
				taxCase.status().name(),
				taxCase.totalSellAmountUsd(),
				taxCase.realizedProfitUsd(),
				taxCase.realizedLossUsd(),
				taxCase.netRealizedPnlUsd(),
				taxCase.taxableRealizedPnlUsd(),
				taxCase.estimatedWithholdingTaxUsd(),
				taxCase.estimatedTreatyTaxUsd(),
				taxCase.estimatedRefundUsd(),
				taxCase.advancePaymentEligible(),
				timestamp(taxCase.updatedAt()),
				taxCase.caseId());
		if (updated == 0) {
			insert(taxCase);
			return;
		}
		replaceMatchedTrades(taxCase);
	}

	private void insert(TaxRefundCase taxCase) {
		jdbcTemplate.update(
				"INSERT INTO tax_refund_cases "
						+ "(case_id, account_id, user_id, tax_year, treaty_country, residence_certificate_file_name, "
						+ "reduced_tax_application_file_name, residence_certificate_document_id, "
						+ "reduced_tax_application_document_id, advance_payment_requested, status, total_sell_amount_usd, "
						+ "realized_profit_usd, realized_loss_usd, net_realized_pnl_usd, taxable_realized_pnl_usd, "
						+ "estimated_withholding_tax_usd, estimated_treaty_tax_usd, estimated_refund_usd, "
						+ "advance_payment_eligible, created_at, updated_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				taxCase.caseId(),
				taxCase.accountId(),
				taxCase.userId(),
				taxCase.taxYear(),
				taxCase.treatyCountry(),
				taxCase.residenceCertificateFileName(),
				taxCase.reducedTaxApplicationFileName(),
				taxCase.residenceCertificateDocumentId(),
				taxCase.reducedTaxApplicationDocumentId(),
				taxCase.advancePaymentRequested(),
				taxCase.status().name(),
				taxCase.totalSellAmountUsd(),
				taxCase.realizedProfitUsd(),
				taxCase.realizedLossUsd(),
				taxCase.netRealizedPnlUsd(),
				taxCase.taxableRealizedPnlUsd(),
				taxCase.estimatedWithholdingTaxUsd(),
				taxCase.estimatedTreatyTaxUsd(),
				taxCase.estimatedRefundUsd(),
				taxCase.advancePaymentEligible(),
				timestamp(taxCase.createdAt()),
				timestamp(taxCase.updatedAt()));
		replaceMatchedTrades(taxCase);
	}

	private void replaceMatchedTrades(TaxRefundCase taxCase) {
		jdbcTemplate.update("DELETE FROM tax_refund_case_matched_trades WHERE case_id = ?", taxCase.caseId());
		for (int index = 0; index < taxCase.matchedTradeIds().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO tax_refund_case_matched_trades (case_id, trade_id, sort_order) "
							+ "VALUES (?, ?, ?)",
					taxCase.caseId(),
					taxCase.matchedTradeIds().get(index),
					index);
		}
	}

	private TaxRefundCase taxCase(ResultSet resultSet) throws SQLException {
		String caseId = resultSet.getString("case_id");
		return new TaxRefundCase(
				caseId,
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getInt("tax_year"),
				resultSet.getString("treaty_country"),
				resultSet.getString("residence_certificate_file_name"),
				resultSet.getString("reduced_tax_application_file_name"),
				resultSet.getString("residence_certificate_document_id"),
				resultSet.getString("reduced_tax_application_document_id"),
				resultSet.getBoolean("advance_payment_requested"),
				TaxRefundCaseStatus.valueOf(resultSet.getString("status")),
				resultSet.getBigDecimal("total_sell_amount_usd"),
				resultSet.getBigDecimal("realized_profit_usd"),
				resultSet.getBigDecimal("realized_loss_usd"),
				resultSet.getBigDecimal("net_realized_pnl_usd"),
				resultSet.getBigDecimal("taxable_realized_pnl_usd"),
				resultSet.getBigDecimal("estimated_withholding_tax_usd"),
				resultSet.getBigDecimal("estimated_treaty_tax_usd"),
				resultSet.getBigDecimal("estimated_refund_usd"),
				resultSet.getBoolean("advance_payment_eligible"),
				matchedTradeIds(caseId),
				instant(resultSet, "created_at"),
				instant(resultSet, "updated_at"));
	}

	private List<String> matchedTradeIds(String caseId) {
		return jdbcTemplate.query(
				"SELECT trade_id FROM tax_refund_case_matched_trades "
						+ "WHERE case_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("trade_id"),
				caseId);
	}

	private String caseSelect() {
		return "SELECT case_id, account_id, user_id, tax_year, treaty_country, residence_certificate_file_name, "
				+ "reduced_tax_application_file_name, residence_certificate_document_id, "
				+ "reduced_tax_application_document_id, advance_payment_requested, status, total_sell_amount_usd, "
				+ "realized_profit_usd, realized_loss_usd, net_realized_pnl_usd, taxable_realized_pnl_usd, "
				+ "estimated_withholding_tax_usd, estimated_treaty_tax_usd, estimated_refund_usd, "
				+ "advance_payment_eligible, created_at, updated_at FROM tax_refund_cases";
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}
}
