package com.hana.exchange.notification.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hana.exchange.config.NotificationPushProperties;
import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;

@Component
public class CompositePushNotificationSender implements PushNotificationSender {

	private final NotificationPushProperties properties;
	private final List<PushProviderClient> providerClients;

	public CompositePushNotificationSender(
			NotificationPushProperties properties,
			List<PushProviderClient> providerClients) {
		this.properties = properties;
		this.providerClients = providerClients;
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		List<String> enabledProviders = properties.enabledProviders();
		if (enabledProviders.isEmpty()) {
			return NotificationDeliveryResult.skipped("NO_PUSH_PROVIDER", "No push provider is enabled");
		}
		NotificationDeliveryResult lastSkipped = null;
		for (String provider : enabledProviders) {
			PushProviderClient providerClient = findProvider(provider);
			if (providerClient == null) {
				lastSkipped = NotificationDeliveryResult.skipped(provider, "Push provider is not configured");
				continue;
			}
			NotificationDeliveryResult result = providerClient.send(notification);
			if (result.status() == NotificationDeliveryStatus.DELIVERED
					|| result.status() == NotificationDeliveryStatus.FAILED) {
				return result;
			}
			lastSkipped = result;
		}
		return lastSkipped == null
				? NotificationDeliveryResult.skipped("NO_PUSH_PROVIDER", "No push provider handled notification")
				: lastSkipped;
	}

	private PushProviderClient findProvider(String provider) {
		return providerClients.stream()
				.filter(providerClient -> providerClient.provider().equals(provider))
				.findFirst()
				.orElse(null);
	}
}
