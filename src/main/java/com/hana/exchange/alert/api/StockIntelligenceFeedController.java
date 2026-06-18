package com.hana.exchange.alert.api;

import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.alert.application.StockIntelligenceFeedService;
import com.hana.exchange.alert.domain.StockIntelligenceFeedResponse;
import com.hana.exchange.common.api.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/stocks/{stockCode}/intelligence")
@Tag(name = "Stock Intelligence", description = "Per-stock news and disclosure intelligence feed APIs")
public class StockIntelligenceFeedController {

	private final StockIntelligenceFeedService stockIntelligenceFeedService;

	public StockIntelligenceFeedController(StockIntelligenceFeedService stockIntelligenceFeedService) {
		this.stockIntelligenceFeedService = stockIntelligenceFeedService;
	}

	@GetMapping
	@Operation(summary = "Get per-stock analyzed news and disclosure intelligence feed")
	public ApiResponse<StockIntelligenceFeedResponse> getFeed(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode) {
		return ApiResponse.success(stockIntelligenceFeedService.getFeed(stockCode));
	}
}
