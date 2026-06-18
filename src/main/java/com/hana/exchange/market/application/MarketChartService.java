package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketHistoryClient;
import com.hana.exchange.market.client.OmniLensMarketHistoryPoint;
import com.hana.exchange.market.client.OmniLensMarketHistoryResponse;
import com.hana.exchange.market.domain.MarketChartPointResponse;
import com.hana.exchange.market.domain.MarketChartResponse;

@Service
public class MarketChartService {

	private final OmniLensMarketHistoryClient historyClient;

	public MarketChartService(OmniLensMarketHistoryClient historyClient) {
		this.historyClient = historyClient;
	}

	public MarketChartResponse getChart(
			String stockCode,
			LocalDate from,
			LocalDate to,
			String interval,
			String currency) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "from must be on or before to");
		}
		OmniLensMarketHistoryResponse history = historyClient.getHistory(stockCode, from, to, interval, currency);
		List<MarketChartPointResponse> points = history.points()
				.stream()
				.map(point -> toPoint(point, history.localCurrency()))
				.toList();
		return new MarketChartResponse(
				history.source(),
				history.stockCode(),
				history.interval(),
				from,
				to,
				history.baseCurrency(),
				history.localCurrency(),
				"en",
				points.size(),
				points,
				Instant.now());
	}

	private MarketChartPointResponse toPoint(OmniLensMarketHistoryPoint point, String localCurrency) {
		return new MarketChartPointResponse(
				point.tradeDate(),
				toText(point.openPriceKrw()),
				toText(point.highPriceKrw()),
				toText(point.lowPriceKrw()),
				toText(point.closePriceKrw()),
				localCurrency,
				toText(point.openLocalCurrencyPrice()),
				toText(point.highLocalCurrencyPrice()),
				toText(point.lowLocalCurrencyPrice()),
				toText(point.closeLocalCurrencyPrice()),
				point.volume(),
				toText(point.tradingValueKrw()),
				point.adjusted());
	}

	private String toText(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}
}
