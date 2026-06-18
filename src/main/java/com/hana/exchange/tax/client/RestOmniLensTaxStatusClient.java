package com.hana.exchange.tax.client;

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
public class RestOmniLensTaxStatusClient implements OmniLensTaxStatusClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;

	public RestOmniLensTaxStatusClient(RestClient omniLensRestClient, ExchangeBackendProperties properties) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
	}

	@Override
	public OmniLensTaxStatusSyncResponse sync(OmniLensTaxStatusSyncRequest request) {
		try {
			OmniLensApiResponse<OmniLensTaxStatusSyncResponse> response = restClient.post()
					.uri("/api/v1/tax/refund-cases/sync")
					.headers(headers -> {
						if (StringUtils.hasText(properties.apiKey())) {
							headers.set(API_KEY_HEADER, properties.apiKey());
						}
					})
					.body(request)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.TAX_STATUS_SYNC_FAILED);
			}
			return response.data();
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.TAX_STATUS_SYNC_FAILED, exception.getMessage());
		}
	}
}
