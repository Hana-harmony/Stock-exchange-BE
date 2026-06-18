package com.hana.exchange.market.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Pattern;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketQuoteService;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market", description = "Korean stock quote APIs for the exchange app")
@Validated
public class MarketQuoteController {

	private final MarketQuoteService marketQuoteService;

	public MarketQuoteController(MarketQuoteService marketQuoteService) {
		this.marketQuoteService = marketQuoteService;
	}

	@GetMapping("/quotes")
	@Operation(summary = "Get Korean stock quote snapshot contract")
	public ApiResponse<MarketQuoteSnapshot> getQuotes() {
		return ApiResponse.success(marketQuoteService.getQuoteSnapshot());
	}

	@GetMapping("/quotes/{stockCode}")
	@Operation(summary = "Get Korean stock quote snapshot from Hana OmniLens API")
	public ApiResponse<MarketQuoteSnapshot> getQuote(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(marketQuoteService.getQuoteSnapshot(stockCode, currency));
	}
}
