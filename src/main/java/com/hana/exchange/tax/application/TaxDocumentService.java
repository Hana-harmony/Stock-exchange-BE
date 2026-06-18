package com.hana.exchange.tax.application;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.TaxDocumentStorageProperties;
import com.hana.exchange.tax.domain.TaxDocument;
import com.hana.exchange.tax.domain.TaxDocumentType;
import com.hana.exchange.tax.domain.TaxDocumentUploadResponse;

@Service
public class TaxDocumentService {

	private final AccountRepository accountRepository;
	private final TaxDocumentRepository taxDocumentRepository;
	private final TaxDocumentStorage taxDocumentStorage;
	private final TaxDocumentStorageProperties properties;
	private final IdGenerator idGenerator;

	public TaxDocumentService(
			AccountRepository accountRepository,
			TaxDocumentRepository taxDocumentRepository,
			TaxDocumentStorage taxDocumentStorage,
			TaxDocumentStorageProperties properties,
			IdGenerator idGenerator) {
		this.accountRepository = accountRepository;
		this.taxDocumentRepository = taxDocumentRepository;
		this.taxDocumentStorage = taxDocumentStorage;
		this.properties = properties;
		this.idGenerator = idGenerator;
	}

	public TaxDocumentUploadResponse upload(String accountId, TaxDocumentType documentType, MultipartFile file) {
		MockUsdAccount account = accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
		byte[] content = content(file);
		String documentId = idGenerator.newTaxDocumentId();
		String storageKey = account.accountId() + "/" + documentType.name().toLowerCase() + "/" + documentId;
		TaxDocument document = new TaxDocument(
				documentId,
				account.accountId(),
				account.userId(),
				documentType,
				originalFileName(file),
				contentType(file),
				content.length,
				sha256(content),
				storageKey,
				Instant.now());
		taxDocumentStorage.store(storageKey, content);
		taxDocumentRepository.save(document);
		return toResponse(document);
	}

	private byte[] content(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.TAX_DOCUMENT_INVALID, "Tax document file is required");
		}
		if (file.getSize() > properties.maxFileSizeBytes()) {
			throw new BusinessException(ErrorCode.TAX_DOCUMENT_INVALID, "Tax document file is too large");
		}
		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.TAX_DOCUMENT_STORAGE_FAILED, exception.getMessage());
		}
	}

	private String originalFileName(MultipartFile file) {
		String fileName = file.getOriginalFilename();
		if (fileName == null || fileName.isBlank()) {
			return "document";
		}
		return fileName.replace("\\", "/").substring(fileName.replace("\\", "/").lastIndexOf('/') + 1);
	}

	private String contentType(MultipartFile file) {
		return file.getContentType() == null || file.getContentType().isBlank()
				? "application/octet-stream"
				: file.getContentType();
	}

	private String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		} catch (Exception exception) {
			throw new IllegalStateException("Tax document hash failed", exception);
		}
	}

	private TaxDocumentUploadResponse toResponse(TaxDocument document) {
		return new TaxDocumentUploadResponse(
				document.documentId(),
				document.accountId(),
				document.documentType().name(),
				document.originalFileName(),
				document.contentType(),
				document.sizeBytes(),
				document.sha256(),
				document.storageKey(),
				document.createdAt());
	}
}
