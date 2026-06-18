package com.hana.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class OmniLensClientConfig {

	@Bean
	RestClient omniLensRestClient(RestClient.Builder builder, ExchangeBackendProperties properties) {
		return builder.baseUrl(properties.baseUrl()).build();
	}

	@Bean
	StandardWebSocketClient omniLensWebSocketClient() {
		return new StandardWebSocketClient();
	}

	@Bean
	TaskScheduler omniLensStreamTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("omnilens-stream-");
		scheduler.initialize();
		return scheduler;
	}
}
