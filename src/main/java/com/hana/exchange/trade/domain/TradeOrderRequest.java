package com.hana.exchange.trade.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TradeOrderRequest(
		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String stockCode,

		@NotNull
		TradeSide side,

		@Min(1)
		long quantity,

		@NotNull
		TradeOrderType orderType,

		@NotNull
		@DecimalMin(value = "0.01")
		BigDecimal limitPriceUsd
) {
}
