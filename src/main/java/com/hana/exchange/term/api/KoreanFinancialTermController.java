package com.hana.exchange.term.api;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.term.application.KoreanFinancialTermService;
import com.hana.exchange.term.domain.KoreanFinancialTermExplainRequest;
import com.hana.exchange.term.domain.KoreanFinancialTermExplanationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/financial-terms")
@Tag(name = "Financial Terms", description = "Korean financial term explanation proxy APIs")
public class KoreanFinancialTermController {

	private final KoreanFinancialTermService termService;

	public KoreanFinancialTermController(KoreanFinancialTermService termService) {
		this.termService = termService;
	}

	@PostMapping("/explain")
	@Operation(summary = "Explain clicked Korean financial term through Hana OmniLens")
	public ApiResponse<KoreanFinancialTermExplanationResponse> explain(
			@Valid @RequestBody KoreanFinancialTermExplainRequest request) {
		return ApiResponse.success(termService.explain(request));
	}
}
