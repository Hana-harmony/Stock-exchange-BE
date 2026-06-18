package com.hana.exchange.tax.domain;

import java.time.Instant;

public record TaxDocumentUploadResponse(
		String documentId,
		String accountId,
		String documentType,
		String originalFileName,
		String contentType,
		long sizeBytes,
		String sha256,
		String storageKey,
		Instant createdAt
) {
}
