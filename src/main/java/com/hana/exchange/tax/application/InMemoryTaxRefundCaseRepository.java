package com.hana.exchange.tax.application;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hana.exchange.tax.domain.TaxRefundCase;

@Repository
public class InMemoryTaxRefundCaseRepository implements TaxRefundCaseRepository {

	private final Map<String, TaxRefundCase> casesByAccountAndYear = new ConcurrentHashMap<>();

	@Override
	public Optional<TaxRefundCase> findByAccountIdAndTaxYear(String accountId, int taxYear) {
		return Optional.ofNullable(casesByAccountAndYear.get(key(accountId, taxYear)));
	}

	@Override
	public Optional<TaxRefundCase> findLatestByAccountId(String accountId) {
		return casesByAccountAndYear.values()
				.stream()
				.filter(taxCase -> taxCase.accountId().equals(accountId))
				.max(Comparator.comparing(TaxRefundCase::taxYear).thenComparing(TaxRefundCase::updatedAt));
	}

	@Override
	public void save(TaxRefundCase taxCase) {
		casesByAccountAndYear.put(key(taxCase.accountId(), taxCase.taxYear()), taxCase);
	}

	private String key(String accountId, int taxYear) {
		return accountId + ":" + taxYear;
	}
}
