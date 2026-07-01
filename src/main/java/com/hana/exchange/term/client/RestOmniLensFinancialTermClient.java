package com.hana.exchange.term.client;

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
public class RestOmniLensFinancialTermClient implements OmniLensFinancialTermClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensFinancialTermClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public OmniLensKoreanFinancialTermExplanation explain(OmniLensKoreanFinancialTermExplainRequest request) {
		try {
			OmniLensApiResponse<OmniLensKoreanFinancialTermExplanation> response =
					retryer.execute("term.explain", () -> restClient.post()
							.uri("/api/v1/korean-financial-terms/explain")
							.headers(headers -> {
								if (StringUtils.hasText(properties.apiKey())) {
									headers.set(API_KEY_HEADER, properties.apiKey());
								}
							})
							.body(request)
							.retrieve()
							.body(new ParameterizedTypeReference<>() {
							}));
			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.TERM_EXPLANATION_UPSTREAM_UNAVAILABLE);
			}
			return response.data();
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.TERM_EXPLANATION_UPSTREAM_UNAVAILABLE, exception.getMessage());
		}
	}
}
