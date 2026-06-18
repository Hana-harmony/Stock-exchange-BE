package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

public record NotificationInboxResponse(
		String accountId,
		int unreadCount,
		int totalCount,
		List<NotificationItemResponse> notifications,
		Instant servedAt
) {
}
