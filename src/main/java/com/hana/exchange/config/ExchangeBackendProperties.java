package com.hana.exchange.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hana.omnilens.api")
public record ExchangeBackendProperties(
		String baseUrl,
		String apiKey,
		Duration quoteCacheTtl,
		Duration quoteCacheStaleTtl,
		Stream stream
) {
	public ExchangeBackendProperties {
		if (quoteCacheTtl == null) {
			quoteCacheTtl = Duration.ofSeconds(3);
		}
		if (quoteCacheStaleTtl == null) {
			quoteCacheStaleTtl = Duration.ofSeconds(30);
		}
		if (stream == null) {
			stream = Stream.defaults();
		}
	}

	public record Stream(
			boolean quoteEnabled,
			String quotePath,
			String quoteCurrency,
			boolean quoteReplayEnabled,
			boolean alertEnabled,
			String alertPath,
			boolean alertReplayEnabled,
			Duration reconnectInitialDelay,
			Duration reconnectMaxDelay,
			int backpressureBufferSize,
			Duration drainInterval
	) {
		public Stream {
			if (quotePath == null || quotePath.isBlank()) {
				quotePath = "/ws/market/quotes";
			}
			if (quoteCurrency == null || quoteCurrency.isBlank()) {
				quoteCurrency = "USD";
			}
			if (alertPath == null || alertPath.isBlank()) {
				alertPath = "/ws/alerts/events";
			}
			if (reconnectInitialDelay == null || reconnectInitialDelay.isNegative() || reconnectInitialDelay.isZero()) {
				reconnectInitialDelay = Duration.ofSeconds(1);
			}
			if (reconnectMaxDelay == null || reconnectMaxDelay.compareTo(reconnectInitialDelay) < 0) {
				reconnectMaxDelay = Duration.ofSeconds(30);
			}
			if (backpressureBufferSize <= 0) {
				backpressureBufferSize = 1000;
			}
			if (drainInterval == null || drainInterval.isNegative() || drainInterval.isZero()) {
				drainInterval = Duration.ofMillis(50);
			}
		}

		public static Stream defaults() {
			return new Stream(
					false,
					"/ws/market/quotes",
					"USD",
					true,
					false,
					"/ws/alerts/events",
					true,
					Duration.ofSeconds(1),
					Duration.ofSeconds(30),
					1000,
					Duration.ofMillis(50));
		}
	}
}
