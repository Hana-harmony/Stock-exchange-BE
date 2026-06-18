package com.hana.exchange.market.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hana.exchange.common.client.OmniLensRestRetryer;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;

@Component
public class RestOmniLensMarketHistoryClient implements OmniLensMarketHistoryClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensMarketHistoryClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public OmniLensMarketHistoryResponse getHistory(
			String stockCode,
			LocalDate from,
			LocalDate to,
			String interval,
			String currency) {
		try {
			OmniLensApiResponse<List<OmniLensMarketDailyPrice>> response = retryer.execute("market.getHistory", () -> restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/market/stocks/{stockCode}/history")
							.queryParam("from", from)
							.queryParam("to", to)
							.build(stockCode))
					.headers(headers -> {
						if (StringUtils.hasText(properties.apiKey())) {
							headers.set(API_KEY_HEADER, properties.apiKey());
						}
					})
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					}));

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
			}
			return toHistoryResponse(stockCode, interval, currency, response.data());
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, exception.getMessage());
		}
	}

	private OmniLensMarketHistoryResponse toHistoryResponse(
			String stockCode,
			String interval,
			String currency,
			List<OmniLensMarketDailyPrice> prices) {
		List<OmniLensMarketHistoryPoint> points = prices.stream()
				.map(this::toHistoryPoint)
				.toList();
		return new OmniLensMarketHistoryResponse(
				stockCode,
				interval,
				"KRW",
				currency,
				points,
				source(prices));
	}

	private OmniLensMarketHistoryPoint toHistoryPoint(OmniLensMarketDailyPrice price) {
		return new OmniLensMarketHistoryPoint(
				price.tradeDate(),
				price.openPriceKrw(),
				price.highPriceKrw(),
				price.lowPriceKrw(),
				price.closePriceKrw(),
				null,
				null,
				null,
				null,
				price.tradingVolume(),
				price.tradingValueKrw(),
				price.adjustedClosePriceKrw() != null);
	}

	private String source(List<OmniLensMarketDailyPrice> prices) {
		return prices.stream()
				.map(OmniLensMarketDailyPrice::source)
				.filter(Objects::nonNull)
				.distinct()
				.reduce((left, right) -> "MIXED_HANA_OMNILENS_HISTORY")
				.orElse("HANA_OMNILENS_KRX_HISTORY");
	}
}
