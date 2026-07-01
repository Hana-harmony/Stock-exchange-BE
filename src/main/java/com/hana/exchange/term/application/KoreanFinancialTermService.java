package com.hana.exchange.term.application;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.hana.exchange.term.client.OmniLensFinancialTermClient;
import com.hana.exchange.term.client.OmniLensFinancialTermEvidence;
import com.hana.exchange.term.client.OmniLensKoreanFinancialTermExplainRequest;
import com.hana.exchange.term.client.OmniLensKoreanFinancialTermExplanation;
import com.hana.exchange.term.domain.FinancialTermEvidenceResponse;
import com.hana.exchange.term.domain.KoreanFinancialTermExplainRequest;
import com.hana.exchange.term.domain.KoreanFinancialTermExplanationResponse;

@Service
public class KoreanFinancialTermService {

	private static final String ANONYMOUS_USER_KEY = "exchange-anonymous-user";

	private final OmniLensFinancialTermClient financialTermClient;

	public KoreanFinancialTermService(OmniLensFinancialTermClient financialTermClient) {
		this.financialTermClient = financialTermClient;
	}

	public KoreanFinancialTermExplanationResponse explain(KoreanFinancialTermExplainRequest request) {
		OmniLensKoreanFinancialTermExplanation explanation = financialTermClient.explain(
				new OmniLensKoreanFinancialTermExplainRequest(
						request.term(),
						request.locale(),
						request.sourceType(),
						request.title(),
						request.context(),
						request.stockCode(),
						request.stockName(),
						request.articleId(),
						request.articleUrl(),
						ANONYMOUS_USER_KEY,
						request.sessionKey(),
						request.allowWebSearch()));
		return toResponse(explanation);
	}

	private KoreanFinancialTermExplanationResponse toResponse(OmniLensKoreanFinancialTermExplanation explanation) {
		return new KoreanFinancialTermExplanationResponse(
				explanation.term(),
				explanation.normalizedTerm(),
				explanation.englishTerm(),
				explanation.category(),
				explanation.definition(),
				explanation.explanation(),
				explanation.example(),
				toText(explanation.confidenceScore()),
				explanation.confidenceLevel(),
				explanation.displayMode(),
				explanation.source(),
				explanation.cacheable(),
				explanation.cacheTtlSeconds(),
				explanation.evidence().stream().map(this::toEvidence).toList(),
				explanation.qualityFlags(),
				explanation.modelVersion(),
				explanation.generatedAt(),
				explanation.cacheHit(),
				explanation.clickCount());
	}

	private FinancialTermEvidenceResponse toEvidence(OmniLensFinancialTermEvidence evidence) {
		return new FinancialTermEvidenceResponse(
				evidence.title(),
				evidence.snippet(),
				evidence.url(),
				evidence.sourceType());
	}

	private String toText(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}
}
