package com.hana.exchange.audit.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.audit.application.AuditEventService;
import com.hana.exchange.audit.domain.AuditEventListResponse;
import com.hana.exchange.common.api.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/audit")
@Tag(name = "Audit", description = "Account audit event APIs")
public class AuditEventController {

	private final AuditEventService auditEventService;

	public AuditEventController(AuditEventService auditEventService) {
		this.auditEventService = auditEventService;
	}

	@GetMapping("/events")
	@Operation(summary = "Get recent account audit events", security = @SecurityRequirement(name = "bearerAuth"))
	public ApiResponse<AuditEventListResponse> getEvents(
			@PathVariable @Pattern(regexp = "ACC-[0-9A-Z]{12}") String accountId) {
		return ApiResponse.success(auditEventService.getEvents(accountId));
	}
}
