package com.hana.exchange.market.api;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.market.application.MarketQuoteStreamPublisher;
import com.hana.exchange.market.domain.MarketQuoteStreamPublishResponse;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/market/stream")
@Tag(name = "Market Stream", description = "Korean stock quote WebSocket stream publishing APIs")
public class MarketQuoteStreamController {

	private final MarketQuoteStreamPublisher publisher;

	public MarketQuoteStreamController(MarketQuoteStreamPublisher publisher) {
		this.publisher = publisher;
	}

	@PostMapping("/quotes")
	@Operation(summary = "Publish a Korean stock quote tick to FE WebSocket topics")
	public ApiResponse<MarketQuoteStreamPublishResponse> publishQuoteTick(
			@Valid @RequestBody MarketQuoteTickRequest request) {
		return ApiResponse.success(publisher.publish(request));
	}
}
