package com.hana.exchange.trade.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.trade.application.TradeOrderabilityService;
import com.hana.exchange.trade.application.TradeService;
import com.hana.exchange.trade.domain.PortfolioResponse;
import com.hana.exchange.trade.domain.PortfolioValuationHistoryResponse;
import com.hana.exchange.trade.domain.TradeLedgerHistoryResponse;
import com.hana.exchange.trade.domain.TradeOrderHistoryResponse;
import com.hana.exchange.trade.domain.TradeOrderPlacementResponse;
import com.hana.exchange.trade.domain.TradeOrderRequest;
import com.hana.exchange.trade.domain.TradeOrderabilityResponse;
import com.hana.exchange.trade.domain.TradeSide;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Trade", description = "Local mock Korean stock trading APIs")
public class TradeController {

	private final TradeService tradeService;
	private final TradeOrderabilityService tradeOrderabilityService;

	public TradeController(TradeService tradeService, TradeOrderabilityService tradeOrderabilityService) {
		this.tradeService = tradeService;
		this.tradeOrderabilityService = tradeOrderabilityService;
	}

	@PostMapping("/trades")
	@Operation(summary = "Place a limit buy or sell order using Hana OmniLens USD quote")
	public ApiResponse<TradeOrderPlacementResponse> executeTrade(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@Valid @RequestBody TradeOrderRequest request) {
		return ApiResponse.success(tradeService.execute(accountId, request));
	}

	@GetMapping("/orders")
	@Operation(summary = "Get limit order history")
	public ApiResponse<TradeOrderHistoryResponse> getOrderHistory(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
		return ApiResponse.success(tradeService.getOrderHistory(accountId, limit));
	}

	@GetMapping("/trades")
	@Operation(summary = "Get mock trade ledger history")
	public ApiResponse<TradeLedgerHistoryResponse> getTradeLedgerHistory(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
		return ApiResponse.success(tradeService.getTradeLedgerHistory(accountId, limit));
	}

	@GetMapping("/portfolio")
	@Operation(summary = "Get mock holdings and realized PnL")
	public ApiResponse<PortfolioResponse> getPortfolio(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(tradeService.getPortfolio(accountId));
	}

	@GetMapping("/portfolio/history")
	@Operation(summary = "Get mock portfolio valuation history")
	public ApiResponse<PortfolioValuationHistoryResponse> getPortfolioHistory(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit) {
		return ApiResponse.success(tradeService.getPortfolioHistory(accountId, limit));
	}

	@GetMapping("/trades/orderability")
	@Operation(summary = "Check mock trade orderability warnings from Hana OmniLens")
	public ApiResponse<TradeOrderabilityResponse> checkOrderability(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam @Pattern(regexp = "\\d{6}") String stockCode,
			@RequestParam TradeSide side,
			@RequestParam @Min(1) long quantity) {
		return ApiResponse.success(tradeOrderabilityService.check(accountId, stockCode, side, quantity));
	}
}
