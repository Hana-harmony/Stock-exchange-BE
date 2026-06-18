package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

public record NotificationDeviceListResponse(
		String accountId,
		int activeCount,
		int totalCount,
		List<NotificationDeviceResponse> devices,
		Instant servedAt
) {
}
