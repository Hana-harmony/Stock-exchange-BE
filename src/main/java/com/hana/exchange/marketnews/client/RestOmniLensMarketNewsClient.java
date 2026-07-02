package com.hana.exchange.marketnews.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hana.exchange.common.client.OmniLensRestRetryer;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.client.OmniLensApiResponse;

@Component
public class RestOmniLensMarketNewsClient implements OmniLensMarketNewsClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensMarketNewsClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public OmniLensMarketNewsListResponse getLatest(int limit) {
		try {
			OmniLensApiResponse<OmniLensMarketNewsListResponse> response =
					retryer.execute("marketNews.getLatest", () -> restClient.get()
							.uri(uriBuilder -> uriBuilder
									.path("/api/v1/market/news")
									.queryParam("limit", limit)
									.build())
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

	@Override
	public OmniLensMarketNewsEvent getByNewsId(String newsId) {
		try {
			OmniLensApiResponse<OmniLensMarketNewsEvent> response =
					retryer.execute("marketNews.getByNewsId", () -> restClient.get()
							.uri("/api/v1/market/news/{newsId}", newsId)
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
