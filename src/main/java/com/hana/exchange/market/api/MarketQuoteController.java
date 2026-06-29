package com.hana.exchange.market.api;

import java.util.List;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketRealtimeSubscriptionService;
import com.hana.exchange.market.application.MarketQuoteService;
import com.hana.exchange.market.client.OmniLensRealtimeSubscriptionResponse;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market", description = "Korean stock quote APIs for the exchange app")
@Validated
public class MarketQuoteController {

	private final MarketQuoteService marketQuoteService;
	private final MarketRealtimeSubscriptionService realtimeSubscriptionService;

	public MarketQuoteController(
			MarketQuoteService marketQuoteService,
			MarketRealtimeSubscriptionService realtimeSubscriptionService) {
		this.marketQuoteService = marketQuoteService;
		this.realtimeSubscriptionService = realtimeSubscriptionService;
	}

	@GetMapping("/quotes")
	@Operation(summary = "Get Korean stock quote snapshots for all, market, or requested stock codes")
	public ApiResponse<MarketQuoteSnapshot> getQuotes(
			@RequestParam(required = false) @Size(max = 100) List<@Pattern(regexp = "\\d{6}") String> stockCodes,
			@RequestParam(required = false) @Pattern(regexp = "KOSPI|KOSDAQ|KONEX|OTHER") String market,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(marketQuoteService.getQuoteSnapshot(stockCodes, market, currency));
	}

	@GetMapping("/quotes/{stockCode}")
	@Operation(summary = "Get Korean stock quote snapshot from Hana OmniLens API")
	public ApiResponse<MarketQuoteSnapshot> getQuote(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(marketQuoteService.getQuoteSnapshot(stockCode, currency));
	}

	@PostMapping("/stocks/{stockCode}/realtime-subscription")
	@Operation(summary = "Request source KIS realtime subscription for one stock")
	public ApiResponse<OmniLensRealtimeSubscriptionResponse> subscribeRealtimeSource(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "REGULAR") @Pattern(regexp = "REGULAR|AFTER_HOURS_REAL") String session) {
		return ApiResponse.success(realtimeSubscriptionService.subscribe(stockCode, session));
	}

	@DeleteMapping("/stocks/{stockCode}/realtime-subscription")
	@Operation(summary = "Cancel source KIS realtime subscription for one stock")
	public ApiResponse<OmniLensRealtimeSubscriptionResponse> unsubscribeRealtimeSource(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "REGULAR") @Pattern(regexp = "REGULAR|AFTER_HOURS_REAL") String session) {
		return ApiResponse.success(realtimeSubscriptionService.unsubscribe(stockCode, session));
	}
}
