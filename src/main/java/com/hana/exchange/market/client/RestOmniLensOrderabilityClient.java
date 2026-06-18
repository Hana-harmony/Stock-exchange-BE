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
import com.hana.exchange.trade.domain.TradeSide;

@Component
public class RestOmniLensOrderabilityClient implements OmniLensOrderabilityClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensOrderabilityClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public OmniLensOrderabilityResponse checkOrderability(String stockCode, TradeSide side, long quantity) {
		try {
			OmniLensApiResponse<OmniLensOrderabilityResponse> response = retryer.execute("market.checkOrderability", () -> restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/market/stocks/{stockCode}/orderability")
							.queryParam("side", side.name())
							.queryParam("quantity", quantity)
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
