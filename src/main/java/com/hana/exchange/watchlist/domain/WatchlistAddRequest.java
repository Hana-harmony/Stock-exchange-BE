package com.hana.exchange.watchlist.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WatchlistAddRequest(
		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String stockCode
) {
}
