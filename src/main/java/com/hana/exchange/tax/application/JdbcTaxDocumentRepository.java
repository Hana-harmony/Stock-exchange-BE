package com.hana.exchange.tax.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hana.exchange.tax.domain.TaxDocument;
import com.hana.exchange.tax.domain.TaxDocumentType;

@Repository
@Profile("!memory")
public class JdbcTaxDocumentRepository implements TaxDocumentRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcTaxDocumentRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void save(TaxDocument document) {
		jdbcTemplate.update(
				"INSERT INTO tax_documents "
						+ "(document_id, account_id, user_id, document_type, original_file_name, content_type, "
						+ "size_bytes, sha256, storage_key, created_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				document.documentId(),
				document.accountId(),
				document.userId(),
				document.documentType().name(),
				document.originalFileName(),
				document.contentType(),
				document.sizeBytes(),
				document.sha256(),
				document.storageKey(),
				Timestamp.from(document.createdAt()));
	}

	@Override
	public Optional<TaxDocument> findByDocumentId(String documentId) {
		return jdbcTemplate.query(
				"SELECT document_id, account_id, user_id, document_type, original_file_name, content_type, "
						+ "size_bytes, sha256, storage_key, created_at FROM tax_documents WHERE document_id = ?",
				(resultSet, rowNumber) -> document(resultSet),
				documentId)
				.stream()
				.findFirst();
	}

	private TaxDocument document(ResultSet resultSet) throws SQLException {
		return new TaxDocument(
				resultSet.getString("document_id"),
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				TaxDocumentType.valueOf(resultSet.getString("document_type")),
				resultSet.getString("original_file_name"),
				resultSet.getString("content_type"),
				resultSet.getLong("size_bytes"),
				resultSet.getString("sha256"),
				resultSet.getString("storage_key"),
				instant(resultSet, "created_at"));
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}
}
