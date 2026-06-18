package com.hana.exchange.market.stream;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.SmartLifecycle;
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

@Component
public class OmniLensMarketQuoteStreamClient implements SmartLifecycle {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final StandardWebSocketClient webSocketClient;
	private final TaskScheduler taskScheduler;
	private final ExchangeBackendProperties properties;
	private final ObjectMapper objectMapper;
	private final OmniLensMarketQuoteStreamMessageHandler messageHandler;
	private final AtomicInteger reconnectAttempts = new AtomicInteger();
	private final AtomicReference<WebSocketSession> session = new AtomicReference<>();
	private volatile boolean running;
	private ScheduledFuture<?> drainTask;
	private ScheduledFuture<?> reconnectTask;

	public OmniLensMarketQuoteStreamClient(
			StandardWebSocketClient omniLensWebSocketClient,
			TaskScheduler omniLensStreamTaskScheduler,
			ExchangeBackendProperties properties,
			ObjectMapper objectMapper,
			OmniLensMarketQuoteStreamMessageHandler messageHandler) {
		this.webSocketClient = omniLensWebSocketClient;
		this.taskScheduler = omniLensStreamTaskScheduler;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.messageHandler = messageHandler;
	}

	@Override
	public void start() {
		if (running || !properties.stream().quoteEnabled()) {
			return;
		}
		running = true;
		drainTask = taskScheduler.scheduleWithFixedDelay(
				messageHandler::drainBufferedTicks,
				properties.stream().drainInterval());
		connect();
	}

	@Override
	public void stop() {
		running = false;
		cancel(drainTask);
		cancel(reconnectTask);
		WebSocketSession openSession = session.getAndSet(null);
		if (openSession != null && openSession.isOpen()) {
			try {
				openSession.close();
			} catch (IOException ignored) {
				// Closing best-effort during application shutdown.
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	public boolean isConnected() {
		WebSocketSession openSession = session.get();
		return openSession != null && openSession.isOpen();
	}

	private void connect() {
		if (!running) {
			return;
		}
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		if (StringUtils.hasText(properties.apiKey())) {
			headers.set(API_KEY_HEADER, properties.apiKey());
		}
		webSocketClient.execute(new QuoteWebSocketHandler(), headers, streamUri())
				.whenComplete((connectedSession, throwable) -> {
					if (throwable != null) {
						scheduleReconnect();
						return;
					}
					session.set(connectedSession);
					reconnectAttempts.set(0);
					sendReplayRequest(connectedSession);
				});
	}

	private void scheduleReconnect() {
		if (!running) {
			return;
		}
		cancel(reconnectTask);
		Duration delay = reconnectDelay(reconnectAttempts.getAndIncrement());
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
		String path = properties.stream().quotePath().startsWith("/")
				? properties.stream().quotePath()
				: "/" + properties.stream().quotePath();
		return URI.create(scheme + withoutScheme + path);
	}

	private void sendReplayRequest(WebSocketSession connectedSession) {
		if (!properties.stream().quoteReplayEnabled()) {
			return;
		}
		Instant replayAfter = messageHandler.replayAfter();
		if (replayAfter == null || !connectedSession.isOpen()) {
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(Map.of(
					"type", "QUOTE_STREAM_REPLAY",
					"currency", properties.stream().quoteCurrency(),
					"after", replayAfter.toString()));
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

	private class QuoteWebSocketHandler extends TextWebSocketHandler {

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			messageHandler.accept(message.getPayload());
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) {
			scheduleReconnect();
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
			OmniLensMarketQuoteStreamClient.this.session.compareAndSet(session, null);
			scheduleReconnect();
		}
	}
}
