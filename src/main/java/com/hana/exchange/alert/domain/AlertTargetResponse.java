package com.hana.exchange.alert.domain;

import java.util.List;

public record AlertTargetResponse(
		String accountId,
		String userId,
		List<String> matchReasons,
		List<String> matchedStockCodes
) {
}
