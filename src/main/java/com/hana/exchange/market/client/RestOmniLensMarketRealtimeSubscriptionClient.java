package com.hana.exchange.market.client;

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
public class RestOmniLensMarketRealtimeSubscriptionClient implements OmniLensMarketRealtimeSubscriptionClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensMarketRealtimeSubscriptionClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public OmniLensRealtimeSubscriptionResponse subscribe(String stockCode, String session) {
		return request("market.subscribeRealtimeSource", "POST", stockCode, session);
	}

	@Override
	public OmniLensRealtimeSubscriptionResponse unsubscribe(String stockCode, String session) {
		return request("market.unsubscribeRealtimeSource", "DELETE", stockCode, session);
	}

	private OmniLensRealtimeSubscriptionResponse request(
			String operationName,
			String method,
			String stockCode,
			String session) {
		try {
			OmniLensApiResponse<OmniLensRealtimeSubscriptionResponse> response = retryer.execute(operationName, () -> {
				RestClient.RequestHeadersSpec<?> spec = "DELETE".equals(method)
						? restClient.delete()
								.uri(uriBuilder -> uriBuilder
										.path("/api/v1/market/stocks/{stockCode}/realtime-subscription")
										.queryParam("session", session)
										.build(stockCode))
						: restClient.post()
								.uri(uriBuilder -> uriBuilder
										.path("/api/v1/market/stocks/{stockCode}/realtime-subscription")
										.queryParam("session", session)
										.build(stockCode));
				return spec
						.headers(headers -> {
							if (StringUtils.hasText(properties.apiKey())) {
								headers.set(API_KEY_HEADER, properties.apiKey());
							}
						})
						.retrieve()
						.body(new ParameterizedTypeReference<>() {
						});
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
