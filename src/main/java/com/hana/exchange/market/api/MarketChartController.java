package com.hana.exchange.market.api;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketChartService;
import com.hana.exchange.market.domain.MarketChartResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/market/stocks/{stockCode}/chart")
@Tag(name = "Market Chart", description = "Korean stock historical chart APIs for the exchange app")
public class MarketChartController {

	private final MarketChartService marketChartService;

	public MarketChartController(MarketChartService marketChartService) {
		this.marketChartService = marketChartService;
	}

	@GetMapping
	@Operation(summary = "Get Korean stock historical chart from Hana OmniLens KRX history API")
	public ApiResponse<MarketChartResponse> getChart(
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "1d") @Pattern(regexp = "1m|1d|1w|1mo") String interval,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(marketChartService.getChart(stockCode, from, to, interval, currency));
	}
}
