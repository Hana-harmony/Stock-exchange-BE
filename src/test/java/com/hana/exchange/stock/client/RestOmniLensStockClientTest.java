package com.hana.exchange.stock.client;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.hana.exchange.config.ExchangeBackendProperties;

class RestOmniLensStockClientTest {

	@Test
	void searchWrapsOmniLensStockSummaryArrayResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensStockClient client = new RestOmniLensStockClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/search?query=%EC%82%BC%EC%84%B1&currency=USD&limit=1"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-HANA-OMNILENS-API-KEY", "test-api-key"))
				.andRespond(withSuccess("""
						{
						  "success": true,
						  "status": 200,
						  "code": "COMMON_000",
						  "message": "OK",
						  "data": [
						    {
						      "stockCode": "005930",
						      "stockName": "삼성전자",
						      "stockNameEn": "Samsung Electronics",
						      "market": "KOSPI",
						      "isinCode": "KR7005930003",
						      "dartCorpCode": "00126380"
						    },
						    {
						      "stockCode": "006400",
						      "stockName": "삼성SDI",
						      "stockNameEn": "Samsung SDI",
						      "market": "KOSPI",
						      "isinCode": "KR7006400006",
						      "dartCorpCode": ""
						    }
						  ],
						  "timestamp": "2026-06-19T16:16:50Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensStockSearchResponse response = client.search("삼성", null, "USD", 1);

		assertThat(response.query()).isEqualTo("삼성");
		assertThat(response.currency()).isEqualTo("USD");
		assertThat(response.source()).isEqualTo("HANA_OMNILENS_API_SEARCH");
		assertThat(response.results()).hasSize(1);
		assertThat(response.results().get(0).stockCode()).isEqualTo("005930");
		assertThat(response.results().get(0).stockNameEn()).isEqualTo("Samsung Electronics");
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
