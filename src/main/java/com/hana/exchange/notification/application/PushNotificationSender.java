package com.hana.exchange.notification.application;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationItem;

public interface PushNotificationSender {

	NotificationDeliveryResult send(NotificationItem notification);
}
