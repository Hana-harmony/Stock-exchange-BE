package com.hana.exchange.tax.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.tax.domain.TaxRefundCase;
import com.hana.exchange.tax.domain.TaxRefundCaseStatus;
import com.hana.exchange.trade.application.TradeRepository;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
class JdbcTaxRefundCaseRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private TaxRefundCaseRepository taxRefundCaseRepository;

	@Test
	void savesFindsAndReplacesTaxRefundCaseInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-TAXDB001",
				"taxdb01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-TAXDB001",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		MockTradeLedgerEntry trade = trade(account, user, "TRD-TAXDB001", now.plusSeconds(1));
		tradeRepository.saveTrade(trade);
		TaxRefundCase taxCase = taxCase(account, user, "TAX-TAXDB001", 2026, List.of(trade.tradeId()), now.plusSeconds(2));

		taxRefundCaseRepository.save(taxCase);

		assertThat(taxRefundCaseRepository.findByAccountIdAndTaxYear(account.accountId(), 2026)).contains(taxCase);
		assertThat(taxRefundCaseRepository.findLatestByAccountId(account.accountId())).contains(taxCase);

		TaxRefundCase replaced = new TaxRefundCase(
				taxCase.caseId(),
				taxCase.accountId(),
				taxCase.userId(),
				taxCase.taxYear(),
				taxCase.treatyCountry(),
				taxCase.residenceCertificateFileName(),
				taxCase.reducedTaxApplicationFileName(),
				taxCase.residenceCertificateDocumentId(),
				taxCase.reducedTaxApplicationDocumentId(),
				false,
				TaxRefundCaseStatus.NO_REFUNDABLE_PROFIT,
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				false,
				List.of(),
				taxCase.createdAt(),
				now.plusSeconds(3));
		taxRefundCaseRepository.save(replaced);

		assertThat(taxRefundCaseRepository.findByAccountIdAndTaxYear(account.accountId(), 2026)).contains(replaced);
	}

	private MockTradeLedgerEntry trade(MockUsdAccount account, ExchangeUser user, String tradeId, Instant executedAt) {
		return new MockTradeLedgerEntry(
				tradeId,
				account.accountId(),
				user.userId(),
				"005930",
				"Samsung Electronics",
				TradeSide.SELL,
				1,
				new BigDecimal("70.00"),
				new BigDecimal("70.00"),
				new BigDecimal("20.00"),
				1,
				new BigDecimal("50.00"),
				new BigDecimal("570.00"),
				executedAt);
	}

	private TaxRefundCase taxCase(
			MockUsdAccount account,
			ExchangeUser user,
			String caseId,
			int taxYear,
			List<String> matchedTradeIds,
			Instant now) {
		return new TaxRefundCase(
				caseId,
				account.accountId(),
				user.userId(),
				taxYear,
				"US",
				"residence-certificate.pdf",
				"reduced-tax-application.pdf",
				null,
				null,
				true,
				TaxRefundCaseStatus.READY_FOR_HANA_SYNC,
				new BigDecimal("70.00"),
				new BigDecimal("20.00"),
				new BigDecimal("0.00"),
				new BigDecimal("20.00"),
				new BigDecimal("20.00"),
				new BigDecimal("4.40"),
				new BigDecimal("3.00"),
				new BigDecimal("1.40"),
				true,
				matchedTradeIds,
				now,
				now);
	}
}
