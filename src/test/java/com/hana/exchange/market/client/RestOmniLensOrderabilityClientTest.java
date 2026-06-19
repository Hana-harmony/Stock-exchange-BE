package com.hana.exchange.market.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hana.exchange.common.client.OmniLensRestRetryer;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.trade.domain.TradeSide;

class RestOmniLensOrderabilityClientTest {

	@Test
	void checkOrderabilityCallsHanaBoundaryAndMapsResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensOrderabilityClient client = new RestOmniLensOrderabilityClient(
				builder.build(),
				properties("test-api-key"),
				retryer("test-api-key"));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/005930/orderability?side=BUY&quantity=20"))
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
						    "side": "BUY",
						    "quantity": 20,
						    "orderable": false,
						    "orderBlockedReason": "FOREIGN_LIMIT_EXCEEDED",
						    "foreignLimitExceeded": true,
						    "currentForeignLimitExhaustionRate": 99.0,
						    "predictedForeignLimitExhaustionRate": 101.0,
							    "foreignOwnershipBaseDate": "2026-06-18",
							    "viActive": true,
							    "singlePriceTrading": true,
							    "priceLimitState": "UPPER_LIMIT",
							    "tradingHalted": false,
						    "checkedAt": "2026-06-18T06:00:00Z",
						    "source": "ORDERABILITY_KIS_WEBSOCKET_TRADE+KRX_FOREIGN_OWNERSHIP_CACHE"
						  },
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		OmniLensOrderabilityResponse response = client.checkOrderability("005930", TradeSide.BUY, 20);

		assertThat(response.stockCode()).isEqualTo("005930");
		assertThat(response.market()).isEqualTo("KOSPI");
		assertThat(response.orderable()).isFalse();
		assertThat(response.orderBlockedReason()).isEqualTo("FOREIGN_LIMIT_EXCEEDED");
		assertThat(response.foreignLimitExceeded()).isTrue();
		assertThat(response.viActive()).isTrue();
		assertThat(response.singlePriceTrading()).isTrue();
		assertThat(response.priceLimitState()).isEqualTo("UPPER_LIMIT");
		assertThat(response.tradingHalted()).isFalse();
		assertThat(response.checkedAt()).isEqualTo(Instant.parse("2026-06-18T06:00:00Z"));
		assertThat(response.source()).isEqualTo("ORDERABILITY_KIS_WEBSOCKET_TRADE+KRX_FOREIGN_OWNERSHIP_CACHE");
		server.verify();
	}

	@Test
	void checkOrderabilityThrowsBusinessExceptionWhenHanaResponseFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://omnilens");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestOmniLensOrderabilityClient client = new RestOmniLensOrderabilityClient(
				builder.build(),
				properties(""),
				retryer(""));

		server.expect(once(), requestTo("http://omnilens/api/v1/market/stocks/005930/orderability?side=SELL&quantity=1"))
				.andRespond(withSuccess("""
						{
						  "success": false,
						  "status": 502,
						  "code": "MARKET_001",
						  "message": "upstream unavailable",
						  "timestamp": "2026-06-18T06:00:01Z"
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.checkOrderability("005930", TradeSide.SELL, 1))
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
