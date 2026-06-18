package com.hana.exchange.account.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record DepositUsdRequest(
		@NotNull
		@DecimalMin(value = "0.01")
		@Digits(integer = 12, fraction = 2)
		BigDecimal amountUsd
) {
}
