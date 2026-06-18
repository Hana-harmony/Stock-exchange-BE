package com.hana.exchange.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record MarketQuoteTickRequest(
		@NotBlank @Pattern(regexp = "\\d{6}") String stockCode,
		@NotBlank String stockName,
		@NotBlank @Pattern(regexp = "KOSPI|KOSDAQ|KONEX|OTHER") String market,
		@NotNull @PositiveOrZero BigDecimal currentPriceKrw,
		@NotNull BigDecimal changeRate,
		@PositiveOrZero long volume,
		@NotBlank @Pattern(regexp = "[A-Z]{3}") String localCurrency,
		@NotNull @PositiveOrZero BigDecimal localCurrencyPrice,
		@NotNull @PositiveOrZero BigDecimal fxRate,
		@NotNull Instant fxRateTime,
		boolean fxStale,
		@NotNull Instant marketDataTime,
		@NotBlank String source
) {
}
