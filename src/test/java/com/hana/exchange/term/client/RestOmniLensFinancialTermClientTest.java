package com.hana.exchange.term.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hana.exchange.common.client.OmniLensRestRetryer;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;

class RestOmniLensFinancialTermClientTest {

	@Test
	void explainCallsOmniLensTermEndpointWithServerApiKey() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensFinancialTermClient client = new RestOmniLensFinancialTermClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));

		server.expect(once(), requestTo("http://omnilens/api/v1/korean-financial-terms/explain"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-HANA-OMNILENS-API-KEY", "test-api-key"))
				.andExpect(content().string(containsString("\"term\":\"retail investors\"")))
				.andExpect(content().string(containsString("\"sourceType\":\"NEWS\"")))
				.andExpect(content().string(containsString("\"userKey\":\"exchange-anonymous-user\"")))
				.andRespond(withSuccess("""
						{
						  "success": true,
						  "status": 200,
						  "code": "COMMON_000",
						  "message": "OK",
						  "data": {
						    "term": "retail investors",
						    "normalizedTerm": "개미",
						    "englishTerm": "retail investor",
						    "category": "market_slang",
						    "definition": "Korean market slang for retail investors.",
						    "explanation": "The term \\"retail investors\\" refers to Korean market slang for individual retail investors.",
						    "example": "In a translated article, \\"retail investors\\" is the clickable local-market term.",
						    "confidenceScore": 0.94,
						    "confidenceLevel": "HIGH",
						    "displayMode": "EXPLANATION",
						    "source": "DICTIONARY",
						    "cacheable": true,
						    "cacheTtlSeconds": 2592000,
						    "evidence": [
						      {
						        "title": "Seed dictionary",
						        "snippet": "Curated glossary.",
						        "url": "",
						        "sourceType": "HANNAH_SEED_GLOSSARY"
						      }
						    ],
						    "qualityFlags": ["DICTIONARY_HIT"],
						    "modelVersion": "k-finance-term-rag-v1",
						    "generatedAt": "2026-07-01T00:00:00Z",
						    "cacheHit": false,
						    "clickCount": 1
						  },
						  "timestamp": "2026-07-01T00:00:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensKoreanFinancialTermExplanation response = client.explain(
				new OmniLensKoreanFinancialTermExplainRequest(
						"retail investors",
						"en",
						"NEWS",
						"Samsung retail investors digest disclosure update",
						"Foreign investors often see retail investors in translated Korean market news.",
						"005930",
						"삼성전자",
						"news-1",
						"https://news.example.com/1",
						"exchange-anonymous-user",
						"session-1",
						true));

		assertThat(response.displayMode()).isEqualTo("EXPLANATION");
		assertThat(response.confidenceScore()).isEqualByComparingTo("0.94");
		assertThat(response.evidence()).hasSize(1);
		server.verify();
	}

	@Test
	void explainThrowsBusinessExceptionWhenEnvelopeFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensFinancialTermClient client = new RestOmniLensFinancialTermClient(
				builder.build(),
				properties(""),
				retryer(""));

		server.expect(once(), requestTo("http://omnilens/api/v1/korean-financial-terms/explain"))
				.andRespond(withSuccess("""
						{
						  "success": false,
						  "status": 502,
						  "code": "TERM_001",
						  "message": "upstream unavailable",
						  "timestamp": "2026-07-01T00:00:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.explain(new OmniLensKoreanFinancialTermExplainRequest(
				"개미",
				"en",
				"NEWS",
				"",
				"",
				null,
				"",
				"",
				"",
				"exchange-anonymous-user",
				"",
				true)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.TERM_EXPLANATION_UPSTREAM_UNAVAILABLE);
		server.verify();
	}

	private ExchangeBackendProperties properties(String apiKey) {
		return new ExchangeBackendProperties(
				"http://omnilens",
				apiKey,
				Duration.ofSeconds(3),
				Duration.ofSeconds(30),
				new ExchangeBackendProperties.Retry(true, 3, Duration.ZERO, Duration.ZERO),
				ExchangeBackendProperties.Stream.defaults());
	}

	private OmniLensRestRetryer retryer(String apiKey) {
		return new OmniLensRestRetryer(properties(apiKey));
	}
}
