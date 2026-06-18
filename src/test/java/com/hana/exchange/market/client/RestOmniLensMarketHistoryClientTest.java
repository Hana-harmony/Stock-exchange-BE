package com.hana.exchange.market.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hana.exchange.common.client.OmniLensRestRetryer;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;

class RestOmniLensMarketHistoryClientTest {

	@Test
	void getHistoryCallsHanaKrxHistoryEndpointAndMapsDailyPrices() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketHistoryClient client = new RestOmniLensMarketHistoryClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/005930/history?from=2026-06-17&to=2026-06-18"))
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
						      "tradeDate": "2026-06-17",
						      "market": "KOSPI",
						      "openPriceKrw": 74000,
						      "highPriceKrw": 75200,
						      "lowPriceKrw": 73800,
						      "closePriceKrw": 75000,
						      "changeRate": 1.25,
						      "tradingVolume": 1000000,
						      "tradingValueKrw": 75000000000,
						      "adjustedClosePriceKrw": 75000,
						      "source": "KRX_OPEN_API_DAILY_TRADE",
						      "collectedAt": "2026-06-18T06:00:00Z"
						    }
						  ],
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensMarketHistoryResponse response = client.getHistory(
				"005930",
				LocalDate.parse("2026-06-17"),
				LocalDate.parse("2026-06-18"),
				"1d",
				"USD");

		assertThat(response.stockCode()).isEqualTo("005930");
		assertThat(response.interval()).isEqualTo("1d");
		assertThat(response.baseCurrency()).isEqualTo("KRW");
		assertThat(response.localCurrency()).isEqualTo("USD");
		assertThat(response.source()).isEqualTo("KRX_OPEN_API_DAILY_TRADE");
		assertThat(response.points()).hasSize(1);
		assertThat(response.points().get(0).openPriceKrw()).isEqualByComparingTo("74000");
		assertThat(response.points().get(0).volume()).isEqualTo(1000000L);
		assertThat(response.points().get(0).tradingValueKrw()).isEqualByComparingTo("75000000000");
		assertThat(response.points().get(0).adjusted()).isTrue();
		server.verify();
	}

	@Test
	void getHistoryThrowsBusinessExceptionWhenHanaResponseFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketHistoryClient client = new RestOmniLensMarketHistoryClient(
				builder.build(),
				properties(""),
				retryer(""));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/005930/history?from=2026-06-17&to=2026-06-18"))
				.andRespond(withSuccess("""
						{
						  "success": false,
						  "status": 502,
						  "code": "MARKET_001",
						  "message": "upstream unavailable",
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getHistory(
				"005930",
				LocalDate.parse("2026-06-17"),
				LocalDate.parse("2026-06-18"),
				"1d",
				"USD"))
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
				new ExchangeBackendProperties.Retry(true, 1, Duration.ZERO, Duration.ZERO),
				ExchangeBackendProperties.Stream.defaults());
	}

	private OmniLensRestRetryer retryer(String apiKey) {
		return new OmniLensRestRetryer(properties(apiKey));
	}
}
