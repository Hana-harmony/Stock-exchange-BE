package com.hana.exchange.alert.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.alert.application.AlertEventService;
import com.hana.exchange.alert.domain.AlertEventIngestRequest;
import com.hana.exchange.alert.domain.AlertEventMatchResponse;
import com.hana.exchange.common.api.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/alerts/events")
@Tag(name = "Alert", description = "News and disclosure alert event matching APIs")
public class AlertEventController {

	private final AlertEventService alertEventService;

	public AlertEventController(AlertEventService alertEventService) {
		this.alertEventService = alertEventService;
	}

	@PostMapping
	@Operation(summary = "Store Hana OmniLens analyzed event and match watchlist or holder targets")
	public ApiResponse<AlertEventMatchResponse> ingestEvent(@Valid @RequestBody AlertEventIngestRequest request) {
		return ApiResponse.success(alertEventService.ingest(request));
	}

	@GetMapping("/{eventId}/targets")
	@Operation(summary = "Get matched target accounts for a stored alert event")
	public ApiResponse<AlertEventMatchResponse> getTargets(@PathVariable String eventId) {
		return ApiResponse.success(alertEventService.getTargets(eventId));
	}
}
