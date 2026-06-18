package com.hana.exchange.tax.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TaxRefundCaseCreateRequest(
		@NotNull @Min(2020) @Max(2100) Integer taxYear,
		@NotBlank @Pattern(regexp = "[A-Z]{2}") String treatyCountry,
		@NotBlank String residenceCertificateFileName,
		@NotBlank String reducedTaxApplicationFileName,
		boolean advancePaymentRequested
) {
}
