package com.hana.exchange.tax.domain;

import java.time.Instant;

public record TaxDocument(
		String documentId,
		String accountId,
		String userId,
		TaxDocumentType documentType,
		String originalFileName,
		String contentType,
		long sizeBytes,
		String sha256,
		String storageKey,
		Instant createdAt
) {
}
