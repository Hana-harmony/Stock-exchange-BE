package com.hana.exchange.stock.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.client.OmniLensApiResponse;

@Component
public class RestOmniLensStockClient implements OmniLensStockClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;

	public RestOmniLensStockClient(RestClient omniLensRestClient, ExchangeBackendProperties properties) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
	}

	@Override
	public OmniLensStockSearchResponse search(String query, String market, String currency, int limit) {
		try {
			OmniLensApiResponse<OmniLensStockSearchResponse> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/market/stocks/search")
							.queryParam("query", query)
							.queryParamIfPresent("market", java.util.Optional.ofNullable(market))
							.queryParam("currency", currency)
							.queryParam("limit", limit)
							.build())
					.headers(headers -> {
						if (StringUtils.hasText(properties.apiKey())) {
							headers.set(API_KEY_HEADER, properties.apiKey());
						}
					})
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
			return data(response);
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, exception.getMessage());
		}
	}

	@Override
	public OmniLensStockDetailResponse getDetail(String stockCode, String currency) {
		try {
			OmniLensApiResponse<OmniLensStockDetailResponse> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/market/stocks/{stockCode}/detail")
							.queryParam("currency", currency)
							.build(stockCode))
					.headers(headers -> {
						if (StringUtils.hasText(properties.apiKey())) {
							headers.set(API_KEY_HEADER, properties.apiKey());
						}
					})
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
			return data(response);
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, exception.getMessage());
		}
	}

	private <T> T data(OmniLensApiResponse<T> response) {
		if (response == null || !response.success() || response.data() == null) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
		}
		return response.data();
	}
}
