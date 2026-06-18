package com.hana.exchange.market.api;

import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketOrderBookService;
import com.hana.exchange.market.domain.MarketOrderBookResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market", description = "Korean stock quote APIs for the exchange app")
@Validated
public class MarketOrderBookController {

	private final MarketOrderBookService marketOrderBookService;

	public MarketOrderBookController(MarketOrderBookService marketOrderBookService) {
		this.marketOrderBookService = marketOrderBookService;
	}

	@GetMapping("/stocks/{stockCode}/orderbook")
	@Operation(summary = "Get Korean stock order book snapshot from Hana OmniLens API")
	public ApiResponse<MarketOrderBookResponse> getOrderBook(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(marketOrderBookService.getOrderBook(stockCode, currency));
	}
}
