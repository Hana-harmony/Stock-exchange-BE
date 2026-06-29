package com.hana.exchange.market.client;

import java.time.LocalDate;
import java.util.List;

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
public class RestOmniLensMarketIntradayClient implements OmniLensMarketIntradayClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensMarketIntradayClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public List<OmniLensMarketIntradayPrice> getIntraday(String stockCode, LocalDate date, int limit) {
		try {
			OmniLensApiResponse<List<OmniLensMarketIntradayPrice>> response =
					retryer.execute("market.getIntraday", () -> restClient.get()
							.uri(uriBuilder -> uriBuilder
									.path("/api/v1/market/stocks/{stockCode}/intraday")
									.queryParam("date", date)
									.queryParam("limit", limit)
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
			return response.data();
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, exception.getMessage());
		}
	}
}
