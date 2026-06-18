package com.hana.exchange.tax.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.tax.application.TaxDocumentService;
import com.hana.exchange.tax.application.TaxRefundCaseService;
import com.hana.exchange.tax.domain.TaxDocumentType;
import com.hana.exchange.tax.domain.TaxDocumentUploadResponse;
import com.hana.exchange.tax.domain.TaxRefundCaseCreateRequest;
import com.hana.exchange.tax.domain.TaxRefundCaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/tax")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tax", description = "Mock trading realized PnL tax refund APIs")
public class TaxRefundCaseController {

	private final TaxRefundCaseService taxRefundCaseService;
	private final TaxDocumentService taxDocumentService;

	public TaxRefundCaseController(TaxRefundCaseService taxRefundCaseService, TaxDocumentService taxDocumentService) {
		this.taxRefundCaseService = taxRefundCaseService;
		this.taxDocumentService = taxDocumentService;
	}

	@PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a tax document to local object storage")
	public ApiResponse<TaxDocumentUploadResponse> uploadDocument(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam TaxDocumentType documentType,
			@RequestParam MultipartFile file) {
		return ApiResponse.success(taxDocumentService.upload(accountId, documentType, file));
	}

	@PostMapping("/refund-cases")
	@Operation(summary = "Create or replace a tax refund case from mock sell realized PnL")
	public ApiResponse<TaxRefundCaseResponse> createRefundCase(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@Valid @RequestBody TaxRefundCaseCreateRequest request) {
		return ApiResponse.success(taxRefundCaseService.createOrReplace(accountId, request));
	}

	@GetMapping("/refund-status")
	@Operation(summary = "Get latest tax refund status for a mock USD account")
	public ApiResponse<TaxRefundCaseResponse> getRefundStatus(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(taxRefundCaseService.getLatestStatus(accountId));
	}

	@PostMapping("/refund-status/sync")
	@Operation(summary = "Sync latest tax refund status with Hana-OmniLens-API")
	public ApiResponse<TaxRefundCaseResponse> syncRefundStatus(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(taxRefundCaseService.syncLatestStatusWithHana(accountId));
	}
}
