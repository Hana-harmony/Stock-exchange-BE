package com.hana.exchange.marketnews.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.marketnews.application.MarketNewsService;
import com.hana.exchange.marketnews.domain.MarketNewsEventResponse;
import com.hana.exchange.marketnews.domain.MarketNewsListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/market/news")
@Tag(name = "Market News", description = "Korean market-wide news proxy APIs")
public class MarketNewsController {

	private final MarketNewsService marketNewsService;

	public MarketNewsController(MarketNewsService marketNewsService) {
		this.marketNewsService = marketNewsService;
	}

	@GetMapping
	@Operation(summary = "Get Korean market-wide news list")
	public ApiResponse<MarketNewsListResponse> getLatest(
			@RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
		return ApiResponse.success(marketNewsService.getLatest(limit));
	}

	@GetMapping("/{newsId}")
	@Operation(summary = "Get Korean market-wide news detail")
	public ApiResponse<MarketNewsEventResponse> getByNewsId(
			@PathVariable @Size(min = 1, max = 80) String newsId) {
		return ApiResponse.success(marketNewsService.getByNewsId(newsId));
	}
}
