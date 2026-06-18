package com.hana.exchange.tax.application;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.tax.domain.TaxDocument;

@Repository
@Profile("memory")
public class InMemoryTaxDocumentRepository implements TaxDocumentRepository {

	private final Map<String, TaxDocument> documents = new ConcurrentHashMap<>();

	@Override
	public void save(TaxDocument document) {
		documents.put(document.documentId(), document);
	}

	@Override
	public Optional<TaxDocument> findByDocumentId(String documentId) {
		return Optional.ofNullable(documents.get(documentId));
	}
}
