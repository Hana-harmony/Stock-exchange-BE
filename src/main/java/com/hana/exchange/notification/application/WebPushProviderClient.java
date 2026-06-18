package com.hana.exchange.notification.application;

import org.springframework.stereotype.Component;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationItem;

@Component
public class WebPushProviderClient implements PushProviderClient {

	@Override
	public String provider() {
		return "WEB_PUSH";
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		return NotificationDeliveryResult.skipped(provider(), "Web push VAPID credentials and send client are not configured");
	}
}
