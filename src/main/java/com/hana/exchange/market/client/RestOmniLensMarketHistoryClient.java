package com.hana.exchange.market.client;

import java.time.LocalDate;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;

@Component
public class RestOmniLensMarketHistoryClient implements OmniLensMarketHistoryClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;

	public RestOmniLensMarketHistoryClient(RestClient omniLensRestClient, ExchangeBackendProperties properties) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
	}

	@Override
	public OmniLensMarketHistoryResponse getHistory(
			String stockCode,
			LocalDate from,
			LocalDate to,
			String interval,
			String currency) {
		try {
			OmniLensApiResponse<OmniLensMarketHistoryResponse> response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/market/stocks/{stockCode}/history")
							.queryParam("from", from)
							.queryParam("to", to)
							.queryParam("interval", interval)
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

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
			}
			return response.data();
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, exception.getMessage());
		}
	}
}
