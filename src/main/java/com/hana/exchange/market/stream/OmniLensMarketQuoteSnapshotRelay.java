package com.hana.exchange.market.stream;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.application.MarketQuoteStreamPublisher;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;

@Component
public class OmniLensMarketQuoteSnapshotRelay implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(OmniLensMarketQuoteSnapshotRelay.class);
	private static final String RELAY_SOURCE_SUFFIX = "+STOCK_EXCHANGE_WS_RELAY";
	private static final List<String> ALLOWED_MARKETS = List.of("KOSPI", "KOSDAQ", "KONEX", "OTHER");

	private final OmniLensMarketQuoteClient quoteClient;
	private final MarketQuoteStreamPublisher publisher;
	private final ExchangeBackendProperties properties;
	private final TaskScheduler taskScheduler;
	private final boolean relayEnabled;
	private final List<String> relayStockCodes;
	private final Duration relayInterval;
	private final String relayCurrency;
	private volatile boolean running;
	private ScheduledFuture<?> relayTask;

	public OmniLensMarketQuoteSnapshotRelay(
			OmniLensMarketQuoteClient quoteClient,
			MarketQuoteStreamPublisher publisher,
			ExchangeBackendProperties properties,
			TaskScheduler omniLensStreamTaskScheduler,
			Environment environment) {
		this.quoteClient = quoteClient;
		this.publisher = publisher;
		this.properties = properties;
		this.taskScheduler = omniLensStreamTaskScheduler;
		this.relayEnabled = relayEnabled(environment);
		this.relayStockCodes = relayStockCodes(environment);
		this.relayInterval = relayInterval(environment);
		this.relayCurrency = relayCurrency(environment);
	}

	@PostConstruct
	void logConfiguration() {
		log.info("Configured OmniLens quote snapshot relay enabled={} stockCodes={} interval={} currency={}",
				relayEnabled,
				relayStockCodes,
				relayInterval,
				relayCurrency);
	}

	@Override
	public void start() {
		if (running || !relayEnabled || relayStockCodes.isEmpty()) {
			log.info("OmniLens quote snapshot relay skipped running={} enabled={} stockCodes={}",
					running,
					relayEnabled,
					relayStockCodes);
			return;
		}
		running = true;
		log.info("Starting OmniLens quote snapshot relay stockCodes={} interval={}",
				relayStockCodes,
				relayInterval);
		relayTask = taskScheduler.scheduleWithFixedDelay(this::relaySafely, relayInterval);
		relaySafely();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startAfterApplicationReady() {
		start();
	}

	@Override
	public void stop() {
		running = false;
		if (relayTask != null) {
			relayTask.cancel(false);
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	int relayOnce() {
		List<OmniLensMarketQuote> quotes = quoteClient.getQuotes(relayStockCodes, relayCurrency);
		quotes.stream()
				.map(this::toTickRequest)
				.forEach(publisher::publish);
		return quotes.size();
	}

	private void relaySafely() {
		if (!running) {
			return;
		}
		try {
			int published = relayOnce();
			if (published == 0) {
				log.debug("OmniLens quote snapshot relay returned no quotes");
			}
		} catch (RuntimeException exception) {
			log.warn("OmniLens quote snapshot relay failed: {}", exception.getMessage());
		}
	}

	private MarketQuoteTickRequest toTickRequest(OmniLensMarketQuote quote) {
		Instant marketDataTime = quote.marketDataTime() == null ? Instant.now() : quote.marketDataTime();
		BigDecimal currentPriceKrw = nonNull(quote.currentPriceKrw());
		BigDecimal localCurrencyPrice = nonNull(quote.localCurrencyPrice());
		BigDecimal fxRate = fxRate(quote, currentPriceKrw, localCurrencyPrice);
		return new MarketQuoteTickRequest(
				quote.stockCode(),
				stockName(quote),
				quote.stockNameEn(),
				market(quote.market()),
				currentPriceKrw,
				nonNull(quote.changeRate()),
				Math.max(quote.volume(), 0L),
				quote.marketSession(),
				quote.afterHoursPriceKrw(),
				quote.afterHoursLocalCurrencyPrice(),
				quote.afterHoursChangeRate(),
				quote.afterHoursVolume(),
				quote.afterHoursMarketDataTime(),
				localCurrency(quote.localCurrency()),
				localCurrencyPrice,
				fxRate,
				quote.fxRateTime() == null ? marketDataTime : quote.fxRateTime(),
				fxRateSource(quote),
				quote.fxStale(),
				marketDataTime,
				source(quote.source()));
	}

	private String stockName(OmniLensMarketQuote quote) {
		if (StringUtils.hasText(quote.stockName())) {
			return quote.stockName();
		}
		if (StringUtils.hasText(quote.stockNameEn())) {
			return quote.stockNameEn();
		}
		return quote.stockCode();
	}

	private String market(String market) {
		if (!StringUtils.hasText(market)) {
			return "OTHER";
		}
		String normalizedMarket = market.toUpperCase(Locale.ROOT);
		return ALLOWED_MARKETS.contains(normalizedMarket) ? normalizedMarket : "OTHER";
	}

	private String localCurrency(String localCurrency) {
		if (StringUtils.hasText(localCurrency)) {
			return localCurrency.toUpperCase(Locale.ROOT);
		}
		return relayCurrency;
	}

	private String fxRateSource(OmniLensMarketQuote quote) {
		if (StringUtils.hasText(quote.fxRateSource())) {
			return quote.fxRateSource();
		}
		return source(quote.source());
	}

	private String source(String source) {
		if (!StringUtils.hasText(source)) {
			return "HANA_OMNILENS_API" + RELAY_SOURCE_SUFFIX;
		}
		return source.contains(RELAY_SOURCE_SUFFIX) ? source : source + RELAY_SOURCE_SUFFIX;
	}

	private BigDecimal fxRate(
			OmniLensMarketQuote quote,
			BigDecimal currentPriceKrw,
			BigDecimal localCurrencyPrice) {
		if (quote.fxRate() != null && quote.fxRate().signum() >= 0) {
			return quote.fxRate();
		}
		if (currentPriceKrw.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return localCurrencyPrice.divide(currentPriceKrw, 10, RoundingMode.HALF_UP);
	}

	private BigDecimal nonNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
	}

	private boolean relayEnabled(Environment environment) {
		return Boolean.parseBoolean(environment.getProperty("HANA_OMNILENS_QUOTE_SNAPSHOT_RELAY_ENABLED", "false"));
	}

	private List<String> relayStockCodes(Environment environment) {
		String csv = environment.getProperty("HANA_OMNILENS_QUOTE_RELAY_STOCK_CODES", "");
		if (!StringUtils.hasText(csv)) {
			return List.of();
		}
		return List.of(csv.split(","))
				.stream()
				.map(String::trim)
				.filter(stockCode -> !stockCode.isBlank())
				.distinct()
				.toList();
	}

	private Duration relayInterval(Environment environment) {
		String raw = environment.getProperty("HANA_OMNILENS_QUOTE_RELAY_INTERVAL", "2s");
		try {
			return DurationStyle.detectAndParse(raw);
		} catch (IllegalArgumentException exception) {
			return Duration.ofSeconds(2);
		}
	}

	private String relayCurrency(Environment environment) {
		String currency = environment.getProperty("HANA_OMNILENS_QUOTE_STREAM_CURRENCY", "USD");
		return StringUtils.hasText(currency) ? currency.toUpperCase(Locale.ROOT) : "USD";
	}
}
