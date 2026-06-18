package com.hana.exchange.trade.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.trade.application.TradeService;
import com.hana.exchange.trade.domain.PortfolioResponse;
import com.hana.exchange.trade.domain.TradeOrderRequest;
import com.hana.exchange.trade.domain.TradeExecutionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
@Tag(name = "Trade", description = "Local mock Korean stock trading APIs")
public class TradeController {

	private final TradeService tradeService;

	public TradeController(TradeService tradeService) {
		this.tradeService = tradeService;
	}

	@PostMapping("/trades")
	@Operation(summary = "Execute a mock buy or sell using Hana OmniLens USD quote")
	public ApiResponse<TradeExecutionResponse> executeTrade(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@Valid @RequestBody TradeOrderRequest request) {
		return ApiResponse.success(tradeService.execute(accountId, request));
	}

	@GetMapping("/portfolio")
	@Operation(summary = "Get mock holdings and realized PnL")
	public ApiResponse<PortfolioResponse> getPortfolio(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(tradeService.getPortfolio(accountId));
	}
}
