package com.hana.exchange.market.api;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketQuoteRealtimeSubscriber;
import com.hana.exchange.market.domain.MarketRealtimeSubscriptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/market/stocks/{stockCode}/realtime-subscription")
@Tag(name = "Market Stream", description = "Korean stock realtime upstream subscription APIs")
@Validated
public class MarketRealtimeSubscriptionController {

	private final MarketQuoteRealtimeSubscriber realtimeSubscriber;

	public MarketRealtimeSubscriptionController(MarketQuoteRealtimeSubscriber realtimeSubscriber) {
		this.realtimeSubscriber = realtimeSubscriber;
	}

	@PostMapping
	@Operation(summary = "Request upstream realtime quote subscription for a stock detail screen")
	public ApiResponse<MarketRealtimeSubscriptionResponse> subscribe(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "REGULAR") @Pattern(regexp = "[A-Z_]+") String session) {
		realtimeSubscriber.requestSubscription(List.of(stockCode), "USD");
		return ApiResponse.success(new MarketRealtimeSubscriptionResponse(
				stockCode,
				session,
				"SUBSCRIBED",
				"HANA_OMNILENS_QUOTE_STREAM",
				Instant.now()));
	}

	@DeleteMapping
	@Operation(summary = "Release local realtime subscription intent for a stock detail screen")
	public ApiResponse<MarketRealtimeSubscriptionResponse> unsubscribe(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "REGULAR") @Pattern(regexp = "[A-Z_]+") String session) {
		return ApiResponse.success(new MarketRealtimeSubscriptionResponse(
				stockCode,
				session,
				"RELEASED_LOCAL",
				"HANA_OMNILENS_QUOTE_STREAM",
				Instant.now()));
	}
}
