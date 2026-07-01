package com.hana.exchange.term.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.term.client.OmniLensFinancialTermClient;
import com.hana.exchange.term.client.OmniLensFinancialTermEvidence;
import com.hana.exchange.term.client.OmniLensKoreanFinancialTermExplanation;

@SpringBootTest
@AutoConfigureMockMvc
class KoreanFinancialTermControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensFinancialTermClient financialTermClient;

	@Test
	void explainReturnsKoreanFinancialTermTooltipPayload() throws Exception {
		when(financialTermClient.explain(any())).thenReturn(termExplanation(false, 1));

		mockMvc.perform(post("/api/v1/financial-terms/explain")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "term": "retail investors",
								  "locale": "en",
								  "sourceType": "NEWS",
								  "title": "Samsung retail investors digest disclosure update",
								  "context": "Foreign investors often see retail investors in translated Korean market news.",
								  "stockCode": "005930",
								  "stockName": "Samsung Electronics",
								  "articleId": "news-1",
								  "articleUrl": "https://news.example.com/1",
								  "sessionKey": "session-1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.term").value("retail investors"))
				.andExpect(jsonPath("$.data.englishTerm").value("retail investor"))
				.andExpect(jsonPath("$.data.confidenceScore").value("0.94"))
				.andExpect(jsonPath("$.data.displayMode").value("EXPLANATION"))
				.andExpect(jsonPath("$.data.cacheHit").value(false))
				.andExpect(jsonPath("$.data.clickCount").value(1))
				.andExpect(jsonPath("$.data.evidence[0].sourceType").value("HANNAH_SEED_GLOSSARY"));
	}

	@Test
	void explainRejectsInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/v1/financial-terms/explain")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "term": "",
								  "locale": "ko",
								  "sourceType": "BLOG",
								  "stockCode": "ABCDEF"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	@Test
	void explainReturnsCommonErrorWhenOmniLensUnavailable() throws Exception {
		when(financialTermClient.explain(any()))
				.thenThrow(new BusinessException(ErrorCode.TERM_EXPLANATION_UPSTREAM_UNAVAILABLE));

		mockMvc.perform(post("/api/v1/financial-terms/explain")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "term": "개미",
								  "locale": "en",
								  "sourceType": "NEWS"
								}
								"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("TERM_001"));
	}

	private OmniLensKoreanFinancialTermExplanation termExplanation(boolean cacheHit, long clickCount) {
		return new OmniLensKoreanFinancialTermExplanation(
				"retail investors",
				"개미",
				"retail investor",
				"market_slang",
				"Korean market slang for individual retail investors.",
				"The term \"retail investors\" refers to Korean market slang for individual retail investors.",
				"In a translated article, \"retail investors\" is the clickable local-market term.",
				new BigDecimal("0.94"),
				"HIGH",
				"EXPLANATION",
				"DICTIONARY",
				true,
				2_592_000,
				List.of(new OmniLensFinancialTermEvidence(
						"Seed dictionary",
						"Curated glossary.",
						"",
						"HANNAH_SEED_GLOSSARY")),
				List.of("DICTIONARY_HIT"),
				"k-finance-term-rag-v1",
				Instant.parse("2026-07-01T00:00:00Z"),
				cacheHit,
				clickCount);
	}
}
