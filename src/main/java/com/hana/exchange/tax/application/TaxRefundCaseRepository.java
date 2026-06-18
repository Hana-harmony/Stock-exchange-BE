package com.hana.exchange.tax.application;

import java.util.Optional;

import com.hana.exchange.tax.domain.TaxRefundCase;

public interface TaxRefundCaseRepository {

	Optional<TaxRefundCase> findByAccountIdAndTaxYear(String accountId, int taxYear);

	Optional<TaxRefundCase> findLatestByAccountId(String accountId);

	void save(TaxRefundCase taxCase);
}
