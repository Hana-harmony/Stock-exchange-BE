package com.hana.exchange.common.client;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.hana.exchange.config.ExchangeBackendProperties;

@Component
public class OmniLensRestRetryer {

	private final ExchangeBackendProperties properties;

	public OmniLensRestRetryer(ExchangeBackendProperties properties) {
		this.properties = properties;
	}

	public <T> T execute(String operationName, Supplier<T> operation) {
		ExchangeBackendProperties.Retry retry = properties.retry();
		int maxAttempts = retry.enabled() ? retry.maxAttempts() : 1;
		RestClientException lastException = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return operation.get();
			} catch (RestClientException exception) {
				lastException = exception;
				if (attempt >= maxAttempts) {
					throw exception;
				}
				sleepBeforeRetry(operationName, attempt, retry);
			}
		}
		throw lastException;
	}

	private void sleepBeforeRetry(String operationName, int attempt, ExchangeBackendProperties.Retry retry) {
		Duration delay = calculateDelay(attempt, retry);
		if (delay.isZero()) {
			return;
		}
		try {
			Thread.sleep(delay.toMillis());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new RestClientException("Interrupted while retrying Hana OmniLens operation " + operationName,
					exception);
		}
	}

	private Duration calculateDelay(int attempt, ExchangeBackendProperties.Retry retry) {
		long multiplier = 1L << Math.min(attempt - 1, 20);
		long initialDelayMillis = retry.initialDelay().toMillis();
		long delayMillis = initialDelayMillis > Long.MAX_VALUE / multiplier
				? Long.MAX_VALUE
				: initialDelayMillis * multiplier;
		long cappedMillis = Math.min(delayMillis, retry.maxDelay().toMillis());
		return Duration.ofMillis(cappedMillis);
	}
}
