package com.hana.exchange.market.api;

import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.AccountMarketQuoteService;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/market/quotes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account Market", description = "Account-scoped Korean stock quote snapshot APIs")
public class AccountMarketQuoteController {

	private final AccountMarketQuoteService accountMarketQuoteService;

	public AccountMarketQuoteController(AccountMarketQuoteService accountMarketQuoteService) {
		this.accountMarketQuoteService = accountMarketQuoteService;
	}

	@GetMapping("/watchlist")
	@Operation(summary = "Get quote snapshots for account watchlist stocks")
	public ApiResponse<MarketQuoteSnapshot> getWatchlistQuotes(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam(required = false) @Pattern(regexp = "KOSPI|KOSDAQ|KONEX|OTHER") String market,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(accountMarketQuoteService.getWatchlistQuotes(accountId, market, currency));
	}

	@GetMapping("/portfolio")
	@Operation(summary = "Get quote snapshots for account portfolio holdings")
	public ApiResponse<MarketQuoteSnapshot> getPortfolioQuotes(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@RequestParam(required = false) @Pattern(regexp = "KOSPI|KOSDAQ|KONEX|OTHER") String market,
			@RequestParam(defaultValue = "USD") @Pattern(regexp = "[A-Z]{3}") String currency) {
		return ApiResponse.success(accountMarketQuoteService.getPortfolioQuotes(accountId, market, currency));
	}
}
