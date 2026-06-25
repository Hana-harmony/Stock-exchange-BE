package com.hana.exchange.market.stream;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.application.MarketIndexStreamPublisher;
import com.hana.exchange.market.client.OmniLensMarketIndex;

@Component
public class OmniLensMarketIndexStreamClient implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(OmniLensMarketIndexStreamClient.class);
	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final StandardWebSocketClient webSocketClient;
	private final TaskScheduler taskScheduler;
	private final ExchangeBackendProperties properties;
	private final ObjectMapper objectMapper;
	private final MarketIndexStreamPublisher publisher;
	private final AtomicInteger reconnectAttempts = new AtomicInteger();
	private final AtomicReference<WebSocketSession> session = new AtomicReference<>();
	private volatile boolean running;
	private ScheduledFuture<?> reconnectTask;

	public OmniLensMarketIndexStreamClient(
			StandardWebSocketClient omniLensWebSocketClient,
			TaskScheduler omniLensStreamTaskScheduler,
			ExchangeBackendProperties properties,
			ObjectMapper objectMapper,
			MarketIndexStreamPublisher publisher) {
		this.webSocketClient = omniLensWebSocketClient;
		this.taskScheduler = omniLensStreamTaskScheduler;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.publisher = publisher;
	}

	@PostConstruct
	void logConfiguration() {
		log.info("Configured OmniLens market index WebSocket stream enabled={} uri={} replayEnabled={}",
				properties.stream().indexEnabled(),
				streamUri(),
				properties.stream().indexReplayEnabled());
	}

	@Override
	public void start() {
		if (running || !properties.stream().indexEnabled()) {
			log.info("OmniLens market index WebSocket stream skipped running={} enabled={}",
					running,
					properties.stream().indexEnabled());
			return;
		}
		running = true;
		log.info("Starting OmniLens market index WebSocket stream client uri={}", streamUri());
		connect();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startAfterApplicationReady() {
		start();
	}

	@Override
	public void stop() {
		running = false;
		cancel(reconnectTask);
		WebSocketSession openSession = session.getAndSet(null);
		if (openSession != null && openSession.isOpen()) {
			try {
				openSession.close();
			} catch (IOException ignored) {
				// 애플리케이션 종료 중 best-effort close.
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	private void connect() {
		if (!running) {
			return;
		}
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		if (StringUtils.hasText(properties.apiKey())) {
			headers.set(API_KEY_HEADER, properties.apiKey());
		}
		webSocketClient.execute(new IndexWebSocketHandler(), headers, streamUri())
				.whenComplete((connectedSession, throwable) -> {
					if (throwable != null) {
						log.warn("OmniLens market index WebSocket connection failed: {}", throwable.toString());
						scheduleReconnect();
						return;
					}
					session.set(connectedSession);
					reconnectAttempts.set(0);
					log.info("OmniLens market index WebSocket connected sessionId={}", connectedSession.getId());
					sendReplayRequest(connectedSession);
				});
	}

	private void scheduleReconnect() {
		if (!running) {
			return;
		}
		cancel(reconnectTask);
		Duration delay = reconnectDelay(reconnectAttempts.getAndIncrement());
		log.info("Scheduling OmniLens market index WebSocket reconnect delay={}", delay);
		reconnectTask = taskScheduler.schedule(this::connect, Instant.now().plus(delay));
	}

	private Duration reconnectDelay(int attempt) {
		Duration initialDelay = properties.stream().reconnectInitialDelay();
		Duration maxDelay = properties.stream().reconnectMaxDelay();
		long multiplier = 1L << Math.min(attempt, 10);
		Duration candidate = initialDelay.multipliedBy(multiplier);
		return candidate.compareTo(maxDelay) > 0 ? maxDelay : candidate;
	}

	private URI streamUri() {
		String baseUrl = properties.baseUrl();
		String scheme = baseUrl.startsWith("https://") ? "wss://" : "ws://";
		String withoutScheme = baseUrl.replaceFirst("^https?://", "");
		String path = properties.stream().indexPath().startsWith("/")
				? properties.stream().indexPath()
				: "/" + properties.stream().indexPath();
		return URI.create(scheme + withoutScheme + path);
	}

	private void sendReplayRequest(WebSocketSession connectedSession) {
		if (!properties.stream().indexReplayEnabled() || !connectedSession.isOpen()) {
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(Map.of("type", "INDEX_STREAM_REPLAY"));
			connectedSession.sendMessage(new TextMessage(payload));
		} catch (IOException ignored) {
			scheduleReconnect();
		}
	}

	private void cancel(ScheduledFuture<?> task) {
		if (task != null) {
			task.cancel(false);
		}
	}

	private class IndexWebSocketHandler extends TextWebSocketHandler {

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
			publisher.publish(objectMapper.readValue(message.getPayload(), OmniLensMarketIndex.class));
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) {
			log.warn("OmniLens market index WebSocket transport error: {}", exception.toString());
			scheduleReconnect();
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
			OmniLensMarketIndexStreamClient.this.session.compareAndSet(session, null);
			log.info("OmniLens market index WebSocket closed status={}", status);
			scheduleReconnect();
		}
	}
}
