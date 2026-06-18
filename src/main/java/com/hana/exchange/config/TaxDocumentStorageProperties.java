package com.hana.exchange.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.tax.document-storage")
public record TaxDocumentStorageProperties(
		Path rootPath,
		long maxFileSizeBytes
) {
	public TaxDocumentStorageProperties {
		if (rootPath == null) {
			rootPath = Path.of("build/local-tax-document-storage");
		}
		if (maxFileSizeBytes <= 0) {
			maxFileSizeBytes = 10 * 1024 * 1024;
		}
	}
}
