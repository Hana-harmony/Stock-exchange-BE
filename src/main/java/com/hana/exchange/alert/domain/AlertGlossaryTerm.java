package com.hana.exchange.alert.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlertGlossaryTerm(
		@NotBlank
		@Size(max = 80)
		String sourceTerm,

		@NotBlank
		@Size(max = 80)
		String normalizedTerm,

		@NotBlank
		@Size(max = 120)
		String englishTerm,

		@NotBlank
		@Size(max = 40)
		String category
) {
}
