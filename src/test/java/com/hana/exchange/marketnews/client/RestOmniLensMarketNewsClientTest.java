package com.hana.exchange.marketnews.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
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

class RestOmniLensMarketNewsClientTest {

	@Test
	void getLatestCallsOmniLensMarketNewsEndpointWithServerApiKey() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketNewsClient client = new RestOmniLensMarketNewsClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/news?limit=5"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-HANA-OMNILENS-API-KEY", "test-api-key"))
				.andRespond(withSuccess("""
						{
						  "success": true,
						  "status": 200,
						  "code": "COMMON_000",
						  "message": "OK",
						  "data": {
						    "newsCount": 1,
						    "news": [
						      {
						        "newsId": "MKT-NEWS-001",
						        "query": "한국 증시",
						        "title": "Ants lift chip bellwethers",
						        "summary": "Ants bought semiconductor bellwethers.",
						        "originalContent": "Ants net bought the KOSPI bellwether.",
						        "imageUrls": ["https://img.example.com/1.jpg"],
						        "contentAvailability": "FULL_TEXT",
						        "originalUrl": "https://news.example.com/1",
						        "canonicalUrl": "https://news.example.com/1",
						        "sourceLicensePolicy": "LINK_ONLY",
						        "duplicateKey": "market-news-1",
						        "publishedAt": "2026-07-02T00:00:00Z",
						        "createdAt": "2026-07-02T00:01:00Z"
						      }
						    ]
						  },
						  "timestamp": "2026-07-02T00:02:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensMarketNewsListResponse response = client.getLatest(5);

		assertThat(response.newsCount()).isEqualTo(1);
		assertThat(response.news()).singleElement()
				.extracting(OmniLensMarketNewsEvent::newsId)
				.isEqualTo("MKT-NEWS-001");
		server.verify();
	}

	@Test
	void getByNewsIdCallsOmniLensMarketNewsDetailEndpoint() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketNewsClient client = new RestOmniLensMarketNewsClient(
				builder.build(),
				properties(""),
				retryer(""));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/news/MKT-NEWS-001"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "success": true,
						  "status": 200,
						  "code": "COMMON_000",
						  "message": "OK",
						  "data": {
						    "newsId": "MKT-NEWS-001",
						    "query": "한국 증시",
						    "title": "Ants lift chip bellwethers",
						    "summary": "Ants bought semiconductor bellwethers.",
						    "originalContent": "Ants net bought the KOSPI bellwether.",
						    "imageUrls": [],
						    "contentAvailability": "FULL_TEXT",
						    "originalUrl": "https://news.example.com/1",
						    "canonicalUrl": "https://news.example.com/1",
						    "sourceLicensePolicy": "LINK_ONLY",
						    "duplicateKey": "market-news-1",
						    "publishedAt": "2026-07-02T00:00:00Z",
						    "createdAt": "2026-07-02T00:01:00Z"
						  },
						  "timestamp": "2026-07-02T00:02:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensMarketNewsEvent response = client.getByNewsId("MKT-NEWS-001");

		assertThat(response.title()).isEqualTo("Ants lift chip bellwethers");
		assertThat(response.originalContent()).contains("Ants");
		server.verify();
	}

	@Test
	void getLatestThrowsBusinessExceptionWhenEnvelopeFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketNewsClient client = new RestOmniLensMarketNewsClient(
				builder.build(),
				properties(""),
				retryer(""));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/news?limit=5"))
				.andRespond(withSuccess("""
						{
						  "success": false,
						  "status": 502,
						  "code": "MARKET_001",
						  "message": "upstream unavailable",
						  "timestamp": "2026-07-02T00:02:00Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getLatest(5))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
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
