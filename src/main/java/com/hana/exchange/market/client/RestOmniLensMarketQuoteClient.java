package com.hana.exchange.market.client;

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
public class RestOmniLensMarketQuoteClient implements OmniLensMarketQuoteClient {

	private static final String API_KEY_HEADER = "X-HANA-OMNILENS-API-KEY";
	private static final int ALL_KOREAN_STOCK_QUOTE_LIMIT = 2000;

	private final RestClient restClient;
	private final ExchangeBackendProperties properties;
	private final OmniLensRestRetryer retryer;

	public RestOmniLensMarketQuoteClient(
			RestClient omniLensRestClient,
			ExchangeBackendProperties properties,
			OmniLensRestRetryer retryer) {
		this.restClient = omniLensRestClient;
		this.properties = properties;
		this.retryer = retryer;
	}

	@Override
	public OmniLensMarketQuote getQuote(String stockCode, String currency) {
		try {
			OmniLensApiResponse<OmniLensMarketQuote> response = retryer.execute("market.getQuote", () -> restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/v1/market/stocks/{stockCode}/quote")
							.queryParam("currency", currency)
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

	@Override
	public List<OmniLensMarketQuote> getAllQuotes(String currency) {
		return getBulkQuotes(List.of(), currency);
	}

	@Override
	public List<OmniLensMarketQuote> getQuotes(List<String> stockCodes, String currency) {
		if (stockCodes == null || stockCodes.isEmpty()) {
			return List.of();
		}
		return getBulkQuotes(stockCodes, currency);
	}

	private List<OmniLensMarketQuote> getBulkQuotes(List<String> stockCodes, String currency) {
		try {
			OmniLensApiResponse<List<OmniLensMarketQuote>> response = retryer.execute("market.getBulkQuotes", () -> restClient.get()
					.uri(uriBuilder -> {
						uriBuilder.path("/api/v1/market/quotes")
								.queryParam("currency", currency);
						if (!stockCodes.isEmpty()) {
							uriBuilder.queryParam("stockCodes", stockCodes.toArray());
						} else {
							uriBuilder.queryParam("limit", ALL_KOREAN_STOCK_QUOTE_LIMIT);
						}
						return uriBuilder.build();
					})
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
