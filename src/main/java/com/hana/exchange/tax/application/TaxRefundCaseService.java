package com.hana.exchange.tax.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.audit.application.AuditEventService;
import com.hana.exchange.audit.domain.AuditEventType;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.tax.domain.TaxMatchedTradeResponse;
import com.hana.exchange.tax.domain.TaxRefundCase;
import com.hana.exchange.tax.domain.TaxRefundCaseCreateRequest;
import com.hana.exchange.tax.domain.TaxRefundCaseResponse;
import com.hana.exchange.tax.domain.TaxRefundCaseStatus;
import com.hana.exchange.trade.application.TradeRepository;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.TradeSide;

@Service
public class TaxRefundCaseService {

	private static final String USD = "USD";
	private static final String DATA_SOURCE = "EXCHANGE_MOCK_LEDGER_REALIZED_PNL";
	private static final BigDecimal LOCAL_WITHHOLDING_RATE = new BigDecimal("0.22");
	private static final BigDecimal TREATY_RATE = new BigDecimal("0.15");

	private final AccountRepository accountRepository;
	private final TradeRepository tradeRepository;
	private final TaxRefundCaseRepository taxRefundCaseRepository;
	private final IdGenerator idGenerator;
	private final AuditEventService auditEventService;

	public TaxRefundCaseService(
			AccountRepository accountRepository,
			TradeRepository tradeRepository,
			TaxRefundCaseRepository taxRefundCaseRepository,
			IdGenerator idGenerator,
			AuditEventService auditEventService) {
		this.accountRepository = accountRepository;
		this.tradeRepository = tradeRepository;
		this.taxRefundCaseRepository = taxRefundCaseRepository;
		this.idGenerator = idGenerator;
		this.auditEventService = auditEventService;
	}

	public TaxRefundCaseResponse createOrReplace(String accountId, TaxRefundCaseCreateRequest request) {
		MockUsdAccount account = account(accountId);
		List<MockTradeLedgerEntry> matchedTrades = matchedSellTrades(accountId, request.taxYear());
		TaxSummary summary = summarize(matchedTrades);
		Instant now = Instant.now();
		TaxRefundCase existingCase = taxRefundCaseRepository
				.findByAccountIdAndTaxYear(accountId, request.taxYear())
				.orElse(null);
		TaxRefundCase taxCase = new TaxRefundCase(
				existingCase == null ? idGenerator.newTaxCaseId() : existingCase.caseId(),
				account.accountId(),
				account.userId(),
				request.taxYear(),
				request.treatyCountry(),
				request.residenceCertificateFileName(),
				request.reducedTaxApplicationFileName(),
				request.advancePaymentRequested(),
				status(summary),
				summary.totalSellAmountUsd(),
				summary.realizedProfitUsd(),
				summary.realizedLossUsd(),
				summary.netRealizedPnlUsd(),
				summary.taxableRealizedPnlUsd(),
				summary.estimatedWithholdingTaxUsd(),
				summary.estimatedTreatyTaxUsd(),
				summary.estimatedRefundUsd(),
				request.advancePaymentRequested() && summary.estimatedRefundUsd().signum() > 0,
				matchedTrades.stream().map(MockTradeLedgerEntry::tradeId).toList(),
				existingCase == null ? now : existingCase.createdAt(),
				now);
		taxRefundCaseRepository.save(taxCase);
		auditEventService.record(
				taxCase.accountId(),
				taxCase.userId(),
				AuditEventType.TAX_REFUND_CASE_UPSERTED,
				"TAX_REFUND_CASE",
				taxCase.caseId(),
				"Tax year " + taxCase.taxYear()
						+ " status=" + taxCase.status().name()
						+ " estimatedRefundUsd=" + moneyText(taxCase.estimatedRefundUsd()),
				taxCase.updatedAt());
		return toResponse(taxCase, matchedTrades);
	}

	public TaxRefundCaseResponse getLatestStatus(String accountId) {
		account(accountId);
		return taxRefundCaseRepository.findLatestByAccountId(accountId)
				.map(taxCase -> toResponse(taxCase, matchedSellTrades(accountId, taxCase.taxYear())))
				.orElseGet(() -> notSubmitted(accountId));
	}

	private TaxRefundCaseResponse notSubmitted(String accountId) {
		MockUsdAccount account = account(accountId);
		Instant now = Instant.now();
		return new TaxRefundCaseResponse(
				null,
				account.accountId(),
				account.userId(),
				now.atZone(ZoneOffset.UTC).getYear(),
				null,
				null,
				null,
				false,
				TaxRefundCaseStatus.NOT_SUBMITTED,
				USD,
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				"0.00",
				false,
				0,
				List.of(),
				DATA_SOURCE,
				null,
				now);
	}

	private MockUsdAccount account(String accountId) {
		return accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
	}

	private List<MockTradeLedgerEntry> matchedSellTrades(String accountId, int taxYear) {
		return tradeRepository.findTrades(accountId)
				.stream()
				.filter(trade -> trade.side() == TradeSide.SELL)
				.filter(trade -> trade.executedAt().atZone(ZoneOffset.UTC).getYear() == taxYear)
				.toList();
	}

	private TaxSummary summarize(List<MockTradeLedgerEntry> trades) {
		BigDecimal totalSellAmount = trades.stream()
				.map(MockTradeLedgerEntry::grossAmountUsd)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal realizedProfit = trades.stream()
				.map(MockTradeLedgerEntry::realizedPnlUsd)
				.filter(value -> value.signum() > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal realizedLoss = trades.stream()
				.map(MockTradeLedgerEntry::realizedPnlUsd)
				.filter(value -> value.signum() < 0)
				.map(BigDecimal::abs)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal netRealizedPnl = realizedProfit.subtract(realizedLoss);
		BigDecimal taxableRealizedPnl = netRealizedPnl.max(BigDecimal.ZERO);
		BigDecimal estimatedWithholdingTax = taxableRealizedPnl.multiply(LOCAL_WITHHOLDING_RATE);
		BigDecimal estimatedTreatyTax = taxableRealizedPnl.multiply(TREATY_RATE);
		BigDecimal estimatedRefund = estimatedWithholdingTax.subtract(estimatedTreatyTax).max(BigDecimal.ZERO);
		return new TaxSummary(
				money(totalSellAmount),
				money(realizedProfit),
				money(realizedLoss),
				money(netRealizedPnl),
				money(taxableRealizedPnl),
				money(estimatedWithholdingTax),
				money(estimatedTreatyTax),
				money(estimatedRefund));
	}

	private TaxRefundCaseStatus status(TaxSummary summary) {
		if (summary.estimatedRefundUsd().signum() <= 0) {
			return TaxRefundCaseStatus.NO_REFUNDABLE_PROFIT;
		}
		return TaxRefundCaseStatus.READY_FOR_HANA_SYNC;
	}

	private TaxRefundCaseResponse toResponse(TaxRefundCase taxCase, List<MockTradeLedgerEntry> matchedTrades) {
		return new TaxRefundCaseResponse(
				taxCase.caseId(),
				taxCase.accountId(),
				taxCase.userId(),
				taxCase.taxYear(),
				taxCase.treatyCountry(),
				taxCase.residenceCertificateFileName(),
				taxCase.reducedTaxApplicationFileName(),
				taxCase.advancePaymentRequested(),
				taxCase.status(),
				USD,
				moneyText(taxCase.totalSellAmountUsd()),
				moneyText(taxCase.realizedProfitUsd()),
				moneyText(taxCase.realizedLossUsd()),
				moneyText(taxCase.netRealizedPnlUsd()),
				moneyText(taxCase.taxableRealizedPnlUsd()),
				moneyText(taxCase.estimatedWithholdingTaxUsd()),
				moneyText(taxCase.estimatedTreatyTaxUsd()),
				moneyText(taxCase.estimatedRefundUsd()),
				taxCase.advancePaymentEligible(),
				matchedTrades.size(),
				matchedTrades.stream().map(this::toMatchedTradeResponse).toList(),
				DATA_SOURCE,
				taxCase.createdAt(),
				taxCase.updatedAt());
	}

	private TaxMatchedTradeResponse toMatchedTradeResponse(MockTradeLedgerEntry trade) {
		return new TaxMatchedTradeResponse(
				trade.tradeId(),
				trade.stockCode(),
				trade.stockName(),
				trade.quantity(),
				moneyText(trade.grossAmountUsd()),
				moneyText(trade.realizedPnlUsd()),
				trade.executedAt());
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private String moneyText(BigDecimal value) {
		return money(value).toPlainString();
	}

	private record TaxSummary(
			BigDecimal totalSellAmountUsd,
			BigDecimal realizedProfitUsd,
			BigDecimal realizedLossUsd,
			BigDecimal netRealizedPnlUsd,
			BigDecimal taxableRealizedPnlUsd,
			BigDecimal estimatedWithholdingTaxUsd,
			BigDecimal estimatedTreatyTaxUsd,
			BigDecimal estimatedRefundUsd
	) {
	}
}
