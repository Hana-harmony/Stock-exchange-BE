package com.hana.exchange.trade.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record MockHolding(
		String accountId,
		String userId,
		String stockCode,
		String stockName,
		long quantity,
		BigDecimal averagePriceUsd,
		BigDecimal costBasisUsd,
		Instant createdAt,
		Instant updatedAt
) {
	public static MockHolding empty(
			String accountId,
			String userId,
			String stockCode,
			String stockName,
			Instant now) {
		return new MockHolding(
				accountId,
				userId,
				stockCode,
				stockName,
				0,
				BigDecimal.ZERO.setScale(2),
				BigDecimal.ZERO.setScale(2),
				now,
				now);
	}

	public MockHolding buy(long buyQuantity, BigDecimal executionPriceUsd, Instant now) {
		BigDecimal buyCost = executionPriceUsd.multiply(BigDecimal.valueOf(buyQuantity)).setScale(2, RoundingMode.HALF_UP);
		long newQuantity = quantity + buyQuantity;
		BigDecimal newCostBasis = costBasisUsd.add(buyCost).setScale(2, RoundingMode.HALF_UP);
		BigDecimal newAveragePrice = newCostBasis.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
		return new MockHolding(
				accountId,
				userId,
				stockCode,
				stockName,
				newQuantity,
				newAveragePrice,
				newCostBasis,
				createdAt,
				now);
	}

	public MockHolding sell(long sellQuantity, Instant now) {
		long newQuantity = quantity - sellQuantity;
		BigDecimal remainingCostBasis = averagePriceUsd.multiply(BigDecimal.valueOf(newQuantity))
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal newAveragePrice = newQuantity == 0 ? BigDecimal.ZERO.setScale(2) : averagePriceUsd;
		return new MockHolding(
				accountId,
				userId,
				stockCode,
				stockName,
				newQuantity,
				newAveragePrice,
				remainingCostBasis,
				createdAt,
				now);
	}
}
