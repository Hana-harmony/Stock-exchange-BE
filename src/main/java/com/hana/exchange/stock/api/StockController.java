package com.hana.exchange.stock.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.stock.application.StockService;
import com.hana.exchange.stock.domain.StockDetailResponse;
import com.hana.exchange.stock.domain.StockSearchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stock", description = "Korean stock search and detail proxy APIs")
public class StockController {

	private final StockService stockService;

	public StockController(StockService stockService) {
		this.stockService = stockService;
	}

	@GetMapping("/search")
	@Operation(summary = "Search Korean listed stocks through Hana OmniLens")
	public ApiResponse<StockSearchResponse> search(
			@RequestParam @Size(min = 1, max = 50) String query,
			@RequestParam(required = false) @Pattern(regexp = "KOSPI|KOSDAQ|KONEX|OTHER") String market,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
		return ApiResponse.success(stockService.search(query, market, currency, limit));
	}

	@GetMapping("/{stockCode}")
	@Operation(summary = "Get Korean stock detail through Hana OmniLens")
	public ApiResponse<StockDetailResponse> getDetail(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(stockService.getDetail(stockCode, currency));
	}
}
