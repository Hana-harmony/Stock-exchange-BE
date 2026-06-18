package com.hana.exchange.notification.application;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationItem;

public interface PushProviderClient {

	String provider();

	NotificationDeliveryResult send(NotificationItem notification);
}
