package com.hana.exchange.notification.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hana.exchange.config.NotificationPushProperties;

@Component
@ConditionalOnProperty(prefix = "exchange.notification.push", name = "worker-enabled", havingValue = "true")
public class NotificationPushWorker {

	private final NotificationService notificationService;
	private final NotificationPushProperties properties;

	public NotificationPushWorker(NotificationService notificationService, NotificationPushProperties properties) {
		this.notificationService = notificationService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "#{@notificationPushProperties.fixedDelay().toMillis()}")
	public void dispatchRetryablePushNotifications() {
		notificationService.dispatchRetryablePushNotifications(properties.batchSize(), properties.maxAttemptCount());
	}
}
