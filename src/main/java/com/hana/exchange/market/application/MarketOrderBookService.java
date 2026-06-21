package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.client.OmniLensOrderBookClient;
import com.hana.exchange.market.client.OmniLensOrderBookLevel;
import com.hana.exchange.market.client.OmniLensOrderBookResponse;
import com.hana.exchange.market.domain.MarketOrderBookResponse;

@Service
public class MarketOrderBookService {

	private final OmniLensOrderBookClient omniLensOrderBookClient;
	private final OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	public MarketOrderBookService(
			OmniLensOrderBookClient omniLensOrderBookClient,
			OmniLensMarketQuoteClient omniLensMarketQuoteClient) {
		this.omniLensOrderBookClient = omniLensOrderBookClient;
		this.omniLensMarketQuoteClient = omniLensMarketQuoteClient;
	}

	public MarketOrderBookResponse getOrderBook(String stockCode, String currency) {
		OmniLensOrderBookResponse orderBook = omniLensOrderBookClient.getOrderBook(stockCode, currency);
		OmniLensMarketQuote quote = omniLensMarketQuoteClient.getQuote(stockCode, currency);
		BigDecimal fxRate = resolveFxRate(quote);
		String displayCurrency = quote.localCurrency() == null || quote.localCurrency().isBlank()
				? currency
				: quote.localCurrency();
		return new MarketOrderBookResponse(
				orderBook.source(),
				orderBook.stockCode(),
				orderBook.market(),
				orderBook.baseCurrency(),
				displayCurrency,
				levels(orderBook.asks(), fxRate),
				levels(orderBook.bids(), fxRate),
				orderBook.marketDataTime(),
				Instant.now());
	}

	private List<MarketOrderBookResponse.Level> levels(List<OmniLensOrderBookLevel> levels, BigDecimal fxRate) {
		if (levels == null) {
			return List.of();
		}
		return levels.stream()
				.map(level -> new MarketOrderBookResponse.Level(
						text(level.priceKrw()),
						text(localPrice(level.priceKrw(), level.localCurrencyPrice(), fxRate)),
						level.quantity(),
						level.orderCount()))
				.toList();
	}

	private BigDecimal localPrice(BigDecimal krwPrice, BigDecimal explicitLocalPrice, BigDecimal fxRate) {
		if (explicitLocalPrice != null) {
			return explicitLocalPrice;
		}
		if (krwPrice == null || fxRate == null) {
			return null;
		}
		return krwPrice.multiply(fxRate);
	}

	private BigDecimal resolveFxRate(OmniLensMarketQuote quote) {
		if (quote.fxRate() != null) {
			return quote.fxRate();
		}
		if (quote.currentPriceKrw() == null
				|| quote.localCurrencyPrice() == null
				|| quote.currentPriceKrw().signum() == 0) {
			throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE, "Hana quote FX rate is missing");
		}
		return quote.localCurrencyPrice().divide(quote.currentPriceKrw(), 10, java.math.RoundingMode.HALF_UP);
	}

	private String text(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}
}
