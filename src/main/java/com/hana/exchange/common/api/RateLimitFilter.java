package com.hana.exchange.common.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.RateLimitProperties;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Pattern ACCOUNT_PATH_PATTERN = Pattern.compile("^/api/v1/accounts/([^/]+)(?:/.*)?$");

	private final RateLimitProperties properties;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final Map<String, WindowCounter> countersByKey = new ConcurrentHashMap<>();

	public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, Clock clock) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !properties.enabled() || !request.getRequestURI().startsWith("/api/v1/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		RateLimitDecision decision = consume(request);
		writeRateLimitHeaders(response, decision);
		if (!decision.allowed()) {
			writeRateLimitError(response, decision);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private RateLimitDecision consume(HttpServletRequest request) {
		long nowMillis = clock.millis();
		long windowMillis = properties.window().toMillis();
		String key = rateLimitKey(request);
		WindowCounter counter = countersByKey.compute(key, (ignored, current) -> {
			if (current == null || nowMillis >= current.resetAtMillis()) {
				return new WindowCounter(1, nowMillis + windowMillis);
			}
			return new WindowCounter(current.count() + 1, current.resetAtMillis());
		});
		boolean allowed = counter.count() <= properties.maxRequests();
		int remaining = Math.max(0, properties.maxRequests() - counter.count());
		long retryAfterSeconds = Math.max(1, (counter.resetAtMillis() - nowMillis + 999) / 1000);
		return new RateLimitDecision(allowed, properties.maxRequests(), remaining, retryAfterSeconds);
	}

	private String rateLimitKey(HttpServletRequest request) {
		Matcher matcher = ACCOUNT_PATH_PATTERN.matcher(request.getRequestURI());
		if (matcher.matches()) {
			return "account:" + matcher.group(1);
		}
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return "ip:" + forwardedFor.split(",")[0].trim();
		}
		return "ip:" + request.getRemoteAddr();
	}

	private void writeRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
		response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
		if (!decision.allowed()) {
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
		}
	}

	private void writeRateLimitError(HttpServletResponse response, RateLimitDecision decision) throws IOException {
		ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
		response.setStatus(errorCode.status().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
		objectMapper.writeValue(response.getWriter(), ApiResponse
				.error(errorCode.status().value(), errorCode.code(), errorCode.message()));
	}

	private record WindowCounter(int count, long resetAtMillis) {
	}

	private record RateLimitDecision(boolean allowed, int limit, int remaining, long retryAfterSeconds) {
	}
}
