package com.hana.exchange.tax.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.TaxDocumentStorageProperties;

@Component
public class LocalTaxDocumentStorage implements TaxDocumentStorage {

	private final TaxDocumentStorageProperties properties;

	public LocalTaxDocumentStorage(TaxDocumentStorageProperties properties) {
		this.properties = properties;
	}

	@Override
	public void store(String storageKey, byte[] content) {
		Path destination = properties.rootPath().resolve(storageKey).normalize();
		if (!destination.startsWith(properties.rootPath().normalize())) {
			throw new BusinessException(ErrorCode.TAX_DOCUMENT_STORAGE_FAILED);
		}
		try {
			Files.createDirectories(destination.getParent());
			Files.write(destination, content);
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.TAX_DOCUMENT_STORAGE_FAILED, exception.getMessage());
		}
	}
}
