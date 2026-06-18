package com.hana.exchange.account.domain;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
		@NotBlank String refreshToken
) {
}
