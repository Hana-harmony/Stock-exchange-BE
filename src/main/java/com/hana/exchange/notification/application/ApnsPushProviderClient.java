package com.hana.exchange.notification.application;

import org.springframework.stereotype.Component;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationItem;

@Component
public class ApnsPushProviderClient implements PushProviderClient {

	@Override
	public String provider() {
		return "APNS_PUSH";
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		return NotificationDeliveryResult.skipped(provider(), "APNS credentials and send client are not configured");
	}
}
