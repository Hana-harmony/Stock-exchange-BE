package com.hana.exchange.market.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketIndexService;
import com.hana.exchange.market.domain.MarketIndexSnapshot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market", description = "Korean market index APIs for the exchange app")
public class MarketIndexController {

	private final MarketIndexService marketIndexService;

	public MarketIndexController(MarketIndexService marketIndexService) {
		this.marketIndexService = marketIndexService;
	}

	@GetMapping("/indices")
	@Operation(summary = "Get realtime Korean market index snapshots")
	public ApiResponse<MarketIndexSnapshot> getIndices() {
		return ApiResponse.success(marketIndexService.getIndexSnapshot());
	}
}
