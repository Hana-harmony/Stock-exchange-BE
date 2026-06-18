package com.hana.exchange.notification.application;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationItem;

@Component
public class LocalNoopPushNotificationSender implements PushNotificationSender {

	private static final String PROVIDER = "LOCAL_NOOP_PUSH";

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		return NotificationDeliveryResult.delivered(PROVIDER, Instant.now());
	}
}
