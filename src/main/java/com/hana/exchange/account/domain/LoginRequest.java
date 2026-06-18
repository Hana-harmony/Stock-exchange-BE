package com.hana.exchange.account.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank
		@Pattern(regexp = "^[A-Za-z0-9_]{4,30}$")
		String username,

		@NotBlank
		@Size(min = 8, max = 72)
		String password
) {
}
