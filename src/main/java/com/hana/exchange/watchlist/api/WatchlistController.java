package com.hana.exchange.watchlist.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.watchlist.application.WatchlistService;
import com.hana.exchange.watchlist.domain.WatchlistAddRequest;
import com.hana.exchange.watchlist.domain.WatchlistResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/watchlist")
@Tag(name = "Watchlist", description = "Local exchange watchlist APIs for quote and alert targeting")
public class WatchlistController {

	private final WatchlistService watchlistService;

	public WatchlistController(WatchlistService watchlistService) {
		this.watchlistService = watchlistService;
	}

	@GetMapping
	@Operation(summary = "Get account watchlist items")
	public ApiResponse<WatchlistResponse> getWatchlist(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(watchlistService.getWatchlist(accountId));
	}

	@PostMapping
	@Operation(summary = "Add Korean stock to watchlist using Hana OmniLens quote metadata")
	public ApiResponse<WatchlistResponse> addWatchlistItem(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@Valid @RequestBody WatchlistAddRequest request) {
		return ApiResponse.success(watchlistService.addItem(accountId, request));
	}

	@DeleteMapping("/{stockCode}")
	@Operation(summary = "Remove Korean stock from watchlist")
	public ApiResponse<WatchlistResponse> removeWatchlistItem(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@PathVariable @Pattern(regexp = "\\d{6}") String stockCode) {
		return ApiResponse.success(watchlistService.removeItem(accountId, stockCode));
	}
}
