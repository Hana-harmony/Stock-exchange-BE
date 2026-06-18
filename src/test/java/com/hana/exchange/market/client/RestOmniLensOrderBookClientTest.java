package com.hana.exchange.market.client;

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

class RestOmniLensOrderBookClientTest {

	@Test
	void getOrderBookCallsHanaOrderBookEndpoint() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensOrderBookClient client = new RestOmniLensOrderBookClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/005930/orderbook?currency=USD"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-HANA-OMNILENS-API-KEY", "test-api-key"))
				.andRespond(withSuccess("""
						{
						  "success": true,
						  "status": 200,
						  "code": "COMMON_000",
						  "message": "OK",
						  "data": {
						    "stockCode": "005930",
						    "market": "KOSPI",
						    "baseCurrency": "KRW",
						    "localCurrency": "USD",
						    "asks": [
						      { "priceKrw": 75000, "localCurrencyPrice": 54.00, "quantity": 1200, "orderCount": 12 }
						    ],
						    "bids": [
						      { "priceKrw": 74900, "localCurrencyPrice": 53.93, "quantity": 900, "orderCount": 9 }
						    ],
						    "marketDataTime": "2026-06-18T06:00:00Z",
						    "source": "HANA_OMNILENS_API"
						  },
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensOrderBookResponse response = client.getOrderBook("005930", "USD");

		assertThat(response.stockCode()).isEqualTo("005930");
		assertThat(response.asks()).hasSize(1);
		assertThat(response.bids()).hasSize(1);
		assertThat(response.asks().get(0).quantity()).isEqualTo(1200);
		server.verify();
	}

	@Test
	void getOrderBookThrowsBusinessExceptionWhenHanaResponseFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensOrderBookClient client = new RestOmniLensOrderBookClient(
				builder.build(),
				properties(""),
				retryer(""));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/005930/orderbook?currency=USD"))
				.andRespond(withSuccess("""
						{
						  "success": false,
						  "status": 502,
						  "code": "MARKET_001",
						  "message": "upstream unavailable",
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getOrderBook("005930", "USD"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
		server.verify();
	}

	private ExchangeBackendProperties properties(String apiKey) {
		return new ExchangeBackendProperties(
				"http://omnilens",
				apiKey,
				Duration.ZERO,
				Duration.ZERO,
				new ExchangeBackendProperties.Retry(true, 1, Duration.ZERO, Duration.ZERO),
				ExchangeBackendProperties.Stream.defaults());
	}

	private OmniLensRestRetryer retryer(String apiKey) {
		return new OmniLensRestRetryer(properties(apiKey));
	}
}
