package com.hana.exchange.notification.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotificationDeviceRegisterRequest(
		@NotNull
		NotificationDevicePlatform platform,

		@NotBlank
		@Pattern(regexp = "FCM_PUSH|APNS_PUSH|WEB_PUSH|LOCAL_NOOP_PUSH")
		String provider,

		@NotBlank
		@Size(min = 16, max = 4096)
		String deviceToken,

		@Size(max = 40)
		String appVersion,

		@Pattern(regexp = "^[A-Za-z]{2}([_-][A-Za-z]{2})?$")
		String locale
) {
}
