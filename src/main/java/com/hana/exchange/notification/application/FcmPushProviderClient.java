package com.hana.exchange.notification.application;

import org.springframework.stereotype.Component;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationItem;

@Component
public class FcmPushProviderClient implements PushProviderClient {

	@Override
	public String provider() {
		return "FCM_PUSH";
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		return NotificationDeliveryResult.skipped(provider(), "FCM device token and credentials are not configured");
	}
}
