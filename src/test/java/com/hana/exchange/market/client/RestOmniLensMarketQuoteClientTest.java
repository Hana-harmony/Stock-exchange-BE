package com.hana.exchange.market.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hana.exchange.common.client.OmniLensRestRetryer;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;

class RestOmniLensMarketQuoteClientTest {

	@Test
	void getQuotesCallsHanaBulkQuoteEndpointWithRepeatedStockCodes() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketQuoteClient client = new RestOmniLensMarketQuoteClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/quotes?currency=USD&stockCodes=005930&stockCodes=000660"))
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
						      "currentPriceKrw": 75000,
						      "changeRate": 1.25,
						      "volume": 1000000,
						      "executionPriceKrw": 75000,
						      "baseCurrency": "KRW",
						      "localCurrencyPrice": 54.00,
						      "localCurrency": "USD",
						      "foreignOwnedQuantity": 50000000,
						      "foreignOwnershipRate": 54.5,
						      "foreignLimitExhaustionRate": 72.3,
						      "foreignOwnershipBaseDate": "2026-06-18",
						      "marketDataTime": "2026-06-18T06:00:00Z",
						      "source": "HANA_OMNILENS_API_BULK"
						    }
						  ],
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		List<OmniLensMarketQuote> quotes = client.getQuotes(List.of("005930", "000660"), "USD");

		assertThat(quotes).hasSize(1);
		assertThat(quotes.get(0).stockCode()).isEqualTo("005930");
		assertThat(quotes.get(0).source()).isEqualTo("HANA_OMNILENS_API_BULK");
		server.verify();
	}

	@Test
	void getAllQuotesCallsHanaAllQuoteEndpointWithoutStockCodes() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketQuoteClient client = new RestOmniLensMarketQuoteClient(
				builder.build(),
				properties(""),
				retryer(""));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/quotes?currency=USD"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{
						  "success": true,
						  "status": 200,
						  "code": "COMMON_000",
						  "message": "OK",
						  "data": [],
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThat(client.getAllQuotes("USD")).isEmpty();
		server.verify();
	}

	@Test
	void getQuotesThrowsBusinessExceptionWhenBulkResponseFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketQuoteClient client = new RestOmniLensMarketQuoteClient(
				builder.build(),
				properties(""),
				retryer(""));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/quotes?currency=USD&stockCodes=005930"))
				.andRespond(withSuccess("""
						{
						  "success": false,
						  "status": 502,
						  "code": "MARKET_001",
						  "message": "upstream unavailable",
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.getQuotes(List.of("005930"), "USD"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
		server.verify();
	}

	@Test
	void getQuotesRetriesRestClientExceptionAndReturnsSuccessfulSecondAttempt() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensMarketQuoteClient client = new RestOmniLensMarketQuoteClient(
				builder.build(),
				properties(""),
				retryer(""));
		server.expect(once(), requestTo("http://omnilens/api/v1/market/quotes?currency=USD&stockCodes=005930"))
				.andRespond(withServerError());
		server.expect(once(), requestTo("http://omnilens/api/v1/market/quotes?currency=USD&stockCodes=005930"))
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
						      "currentPriceKrw": 75000,
						      "changeRate": 1.25,
						      "volume": 1000000,
						      "executionPriceKrw": 75000,
						      "baseCurrency": "KRW",
						      "localCurrencyPrice": 54.00,
						      "localCurrency": "USD",
						      "foreignOwnedQuantity": 50000000,
						      "foreignOwnershipRate": 54.5,
						      "foreignLimitExhaustionRate": 72.3,
						      "foreignOwnershipBaseDate": "2026-06-18",
						      "marketDataTime": "2026-06-18T06:00:00Z",
						      "source": "HANA_OMNILENS_API_BULK"
						    }
						  ],
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		List<OmniLensMarketQuote> quotes = client.getQuotes(List.of("005930"), "USD");

		assertThat(quotes).hasSize(1);
		assertThat(quotes.get(0).stockCode()).isEqualTo("005930");
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
