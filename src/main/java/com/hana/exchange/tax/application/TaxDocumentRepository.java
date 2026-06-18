package com.hana.exchange.tax.application;

import java.util.Optional;

import com.hana.exchange.tax.domain.TaxDocument;

public interface TaxDocumentRepository {

	void save(TaxDocument document);

	Optional<TaxDocument> findByDocumentId(String documentId);
}
