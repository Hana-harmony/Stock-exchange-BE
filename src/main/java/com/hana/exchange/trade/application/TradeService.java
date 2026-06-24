package com.hana.exchange.trade.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.audit.application.AuditEventService;
import com.hana.exchange.audit.domain.AuditEventType;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;
import com.hana.exchange.stock.application.StockDisplayNameFormatter;
import com.hana.exchange.trade.domain.HoldingResponse;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.PendingLimitOrder;
import com.hana.exchange.trade.domain.PortfolioResponse;
import com.hana.exchange.trade.domain.PortfolioValuationHistoryItemResponse;
import com.hana.exchange.trade.domain.PortfolioValuationHistoryResponse;
import com.hana.exchange.trade.domain.PortfolioValuationSnapshot;
import com.hana.exchange.trade.domain.TradeExecutionResponse;
import com.hana.exchange.trade.domain.TradeLedgerHistoryResponse;
import com.hana.exchange.trade.domain.TradeOrderHistoryResponse;
import com.hana.exchange.trade.domain.TradeOrderPlacementResponse;
import com.hana.exchange.trade.domain.TradeOrderRequest;
import com.hana.exchange.trade.domain.TradeOrderStatus;
import com.hana.exchange.trade.domain.TradeOrderType;
import com.hana.exchange.trade.domain.TradeSide;

@Service
public class TradeService {

	private static final String USD = "USD";
	private static final String TRADING_MODE = "EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING";
	private static final int RECENT_TRADE_LIMIT = 20;
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
	private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

	private final AccountRepository accountRepository;
	private final TradeRepository tradeRepository;
	private final OmniLensMarketQuoteClient quoteClient;
	private final TradeOrderabilityService tradeOrderabilityService;
	private final IdGenerator idGenerator;
	private final AuditEventService auditEventService;
	private final Clock clock;

	public TradeService(
			AccountRepository accountRepository,
			TradeRepository tradeRepository,
			OmniLensMarketQuoteClient quoteClient,
			TradeOrderabilityService tradeOrderabilityService,
			IdGenerator idGenerator,
			AuditEventService auditEventService,
			Clock clock) {
		this.accountRepository = accountRepository;
		this.tradeRepository = tradeRepository;
		this.quoteClient = quoteClient;
		this.tradeOrderabilityService = tradeOrderabilityService;
		this.idGenerator = idGenerator;
		this.auditEventService = auditEventService;
		this.clock = clock;
	}

	public TradeOrderPlacementResponse execute(String accountId, TradeOrderRequest request) {
		Instant now = now();
		ensureMarketOpen(now);
		if (request.orderType() != TradeOrderType.LIMIT) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_ORDER_TYPE);
		}
		if (!tradeOrderabilityService.check(accountId, request.stockCode(), request.side(), request.quantity()).canPlaceMockOrder()) {
			throw new BusinessException(ErrorCode.MOCK_ORDER_BLOCKED);
		}
		MockUsdAccount account = account(accountId);
		OmniLensMarketQuote quote = quoteClient.getQuote(request.stockCode(), USD);
		BigDecimal observedPriceUsd = money(quote.localCurrencyPrice());
		BigDecimal limitPriceUsd = money(request.limitPriceUsd());
		validateLimitOrderBalance(account, quote, request.side(), request.quantity(), limitPriceUsd);
		PendingLimitOrder order = new PendingLimitOrder(
				idGenerator.newOrderId(),
				account.accountId(),
				account.userId(),
				quote.stockCode(),
				displayName(quote),
				request.side(),
				request.quantity(),
				limitPriceUsd,
				observedPriceUsd,
				TradeOrderStatus.PENDING,
				null,
				now,
				null);

		if (!limitReached(request.side(), observedPriceUsd, limitPriceUsd)) {
			tradeRepository.saveLimitOrder(order);
			return toOrderResponse(order, null, "Limit order is waiting for the market price to reach the limit.");
		}

		BigDecimal grossAmountUsd = money(observedPriceUsd.multiply(BigDecimal.valueOf(request.quantity())));
		ExecutionResult result = executeSide(account, quote, request.side(), request.quantity(), observedPriceUsd, grossAmountUsd, now);
		PendingLimitOrder filled = order.filled(result.trade().tradeId(), observedPriceUsd, now);
		tradeRepository.saveLimitOrder(filled);
		return toOrderResponse(filled, result.response(), "Limit order filled at the current market price.");
	}

	public int processLimitOrders(MarketQuoteTickRequest request) {
		Instant now = now();
		if (!isMarketOpen(now)) {
			return 0;
		}
		BigDecimal observedPriceUsd = money(request.localCurrencyPrice());
		int filledCount = 0;
		for (PendingLimitOrder order : tradeRepository.findPendingLimitOrdersByStockCode(request.stockCode())) {
			if (!limitReached(order.side(), observedPriceUsd, order.limitPriceUsd())) {
				continue;
			}
			try {
				MockUsdAccount account = account(order.accountId());
				OmniLensMarketQuote quote = quoteFromTick(request);
				BigDecimal grossAmountUsd = money(observedPriceUsd.multiply(BigDecimal.valueOf(order.quantity())));
				ExecutionResult result = executeSide(account, quote, order.side(), order.quantity(), observedPriceUsd, grossAmountUsd, now);
				tradeRepository.saveLimitOrder(order.filled(result.trade().tradeId(), observedPriceUsd, now));
				filledCount++;
			} catch (BusinessException ignored) {
			}
		}
		return filledCount;
	}

	public PortfolioResponse getPortfolio(String accountId) {
		MockUsdAccount account = account(accountId);
		List<MockHolding> currentHoldings = tradeRepository.findHoldings(accountId);
		Map<String, OmniLensMarketQuote> quotesByStockCode = quoteMap(currentHoldings);
		List<HoldingResponse> holdings = currentHoldings
				.stream()
				.map(holding -> toHoldingResponse(holding, quotesByStockCode.get(holding.stockCode())))
				.toList();
		List<TradeExecutionResponse> recentTrades = tradeRepository.findRecentTrades(accountId, RECENT_TRADE_LIMIT)
				.stream()
				.map(this::toTradeResponse)
				.toList();
		BigDecimal realizedPnl = tradeRepository.findTrades(accountId)
				.stream()
				.map(MockTradeLedgerEntry::realizedPnlUsd)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMarketValue = holdings.stream()
				.map(holding -> new BigDecimal(holding.marketValueUsd()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unrealizedPnl = holdings.stream()
				.map(holding -> new BigDecimal(holding.unrealizedPnlUsd()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		Instant servedAt = now();
		PortfolioResponse response = new PortfolioResponse(
				account.userId(),
				account.accountId(),
				USD,
				moneyText(account.cashBalanceUsd()),
				moneyText(totalMarketValue),
				moneyText(account.cashBalanceUsd().add(totalMarketValue)),
				moneyText(realizedPnl),
				moneyText(unrealizedPnl),
				TRADING_MODE,
				holdings,
				recentTrades,
				servedAt);
		tradeRepository.savePortfolioValuationSnapshot(new PortfolioValuationSnapshot(
				idGenerator.newPortfolioValuationSnapshotId(),
				account.accountId(),
				account.userId(),
				USD,
				money(account.cashBalanceUsd()),
				money(totalMarketValue),
				money(account.cashBalanceUsd().add(totalMarketValue)),
				money(realizedPnl),
				money(unrealizedPnl),
				holdings.size(),
				servedAt));
		return response;
	}

	public PortfolioValuationHistoryResponse getPortfolioHistory(String accountId, int limit) {
		account(accountId);
		List<PortfolioValuationHistoryItemResponse> snapshots = tradeRepository
				.findPortfolioValuationSnapshots(accountId, limit)
				.stream()
				.map(this::toPortfolioValuationHistoryItemResponse)
				.toList();
		return new PortfolioValuationHistoryResponse(
				accountId,
				USD,
				snapshots.size(),
				snapshots,
				now());
	}

	public TradeLedgerHistoryResponse getTradeLedgerHistory(String accountId, int limit) {
		account(accountId);
		List<TradeExecutionResponse> trades = tradeRepository.findRecentTrades(accountId, limit)
				.stream()
				.map(this::toTradeResponse)
				.toList();
		return new TradeLedgerHistoryResponse(
				accountId,
				trades.size(),
				trades,
				now());
	}

	public TradeOrderHistoryResponse getOrderHistory(String accountId, int limit) {
		account(accountId);
		List<TradeOrderPlacementResponse> orders = tradeRepository.findRecentLimitOrders(accountId, limit)
				.stream()
				.map(order -> toOrderResponse(order, null, order.status() == TradeOrderStatus.FILLED
						? "Limit order filled."
						: "Limit order is waiting for the market price to reach the limit."))
				.toList();
		return new TradeOrderHistoryResponse(
				accountId,
				orders.size(),
				orders,
				now());
	}

	private ExecutionResult executeSide(
			MockUsdAccount account,
			OmniLensMarketQuote quote,
			TradeSide side,
			long quantity,
			BigDecimal executionPriceUsd,
			BigDecimal grossAmountUsd,
			Instant now) {
		if (side == TradeSide.BUY) {
			return buy(account, quote, quantity, executionPriceUsd, grossAmountUsd, now);
		}
		return sell(account, quote, quantity, executionPriceUsd, grossAmountUsd, now);
	}

	private ExecutionResult buy(
			MockUsdAccount account,
			OmniLensMarketQuote quote,
			long quantity,
			BigDecimal executionPriceUsd,
			BigDecimal grossAmountUsd,
			Instant now) {
		if (account.cashBalanceUsd().compareTo(grossAmountUsd) < 0) {
			throw new BusinessException(ErrorCode.MOCK_ACCOUNT_INSUFFICIENT_BALANCE);
		}

		MockHolding currentHolding = tradeRepository.findHolding(account.accountId(), quote.stockCode())
				.orElse(MockHolding.empty(account.accountId(), account.userId(), quote.stockCode(), displayName(quote), now));
		MockHolding updatedHolding = currentHolding.buy(quantity, executionPriceUsd, now);
		MockUsdAccount updatedAccount = account.withdraw(grossAmountUsd, now);
		MockTradeLedgerEntry trade = tradeLedger(
				updatedAccount,
				updatedHolding,
				TradeSide.BUY,
				quantity,
				executionPriceUsd,
				grossAmountUsd,
				BigDecimal.ZERO.setScale(2),
				now);
		accountRepository.saveAccount(updatedAccount);
		tradeRepository.saveHolding(updatedHolding);
		tradeRepository.saveTrade(trade);
		recordTradeAudit(trade);
		return new ExecutionResult(trade, toTradeResponse(trade, updatedAccount.cashBalanceUsd()));
	}

	private ExecutionResult sell(
			MockUsdAccount account,
			OmniLensMarketQuote quote,
			long quantity,
			BigDecimal executionPriceUsd,
			BigDecimal grossAmountUsd,
			Instant now) {
		MockHolding currentHolding = tradeRepository.findHolding(account.accountId(), quote.stockCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_HOLDING_INSUFFICIENT_QUANTITY));
		if (currentHolding.quantity() < quantity) {
			throw new BusinessException(ErrorCode.MOCK_HOLDING_INSUFFICIENT_QUANTITY);
		}

		BigDecimal realizedPnl = money(executionPriceUsd.subtract(currentHolding.averagePriceUsd())
				.multiply(BigDecimal.valueOf(quantity)));
		MockHolding updatedHolding = currentHolding.sell(quantity, now);
		MockUsdAccount updatedAccount = account.deposit(grossAmountUsd, now);
		MockTradeLedgerEntry trade = tradeLedger(
				updatedAccount,
				updatedHolding,
				TradeSide.SELL,
				quantity,
				executionPriceUsd,
				grossAmountUsd,
				realizedPnl,
				now);
		accountRepository.saveAccount(updatedAccount);
		tradeRepository.saveHolding(updatedHolding);
		tradeRepository.saveTrade(trade);
		recordTradeAudit(trade);
		return new ExecutionResult(trade, toTradeResponse(trade, updatedAccount.cashBalanceUsd()));
	}

	private void recordTradeAudit(MockTradeLedgerEntry trade) {
		auditEventService.record(
				trade.accountId(),
				trade.userId(),
				AuditEventType.TRADE_EXECUTED,
				"TRADE",
				trade.tradeId(),
				trade.side().name() + " " + trade.quantity() + " " + trade.stockCode()
						+ " grossUsd=" + moneyText(trade.grossAmountUsd())
						+ " realizedPnlUsd=" + moneyText(trade.realizedPnlUsd()),
				trade.executedAt());
	}

	private MockTradeLedgerEntry tradeLedger(
			MockUsdAccount account,
			MockHolding holding,
			TradeSide side,
			long quantity,
			BigDecimal executionPriceUsd,
			BigDecimal grossAmountUsd,
			BigDecimal realizedPnlUsd,
			Instant now) {
		return new MockTradeLedgerEntry(
				idGenerator.newTradeId(),
				account.accountId(),
				account.userId(),
				holding.stockCode(),
				holding.stockName(),
				side,
				quantity,
				executionPriceUsd,
				grossAmountUsd,
				realizedPnlUsd,
				holding.quantity(),
				holding.averagePriceUsd(),
				account.cashBalanceUsd(),
				now);
	}

	private MockUsdAccount account(String accountId) {
		return accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
	}

	private Map<String, OmniLensMarketQuote> quoteMap(List<MockHolding> holdings) {
		if (holdings.isEmpty()) {
			return Map.of();
		}
		List<String> stockCodes = holdings.stream()
				.map(MockHolding::stockCode)
				.toList();
		return quoteClient.getQuotes(stockCodes, USD)
				.stream()
				.collect(Collectors.toMap(OmniLensMarketQuote::stockCode, Function.identity()));
	}

	private HoldingResponse toHoldingResponse(MockHolding holding, OmniLensMarketQuote quote) {
		BigDecimal currentPrice = quote == null ? holding.averagePriceUsd() : money(quote.localCurrencyPrice());
		BigDecimal marketValue = money(currentPrice.multiply(BigDecimal.valueOf(holding.quantity())));
		BigDecimal unrealizedPnl = money(marketValue.subtract(holding.costBasisUsd()));
		return new HoldingResponse(
				holding.stockCode(),
				holding.stockName(),
				holding.quantity(),
				moneyText(holding.averagePriceUsd()),
				moneyText(holding.costBasisUsd()),
				moneyText(currentPrice),
				moneyText(marketValue),
				moneyText(unrealizedPnl),
				rateText(unrealizedPnl, holding.costBasisUsd()),
				quote == null ? null : quote.marketDataTime(),
				holding.updatedAt());
	}

	private TradeExecutionResponse toTradeResponse(MockTradeLedgerEntry trade) {
		return toTradeResponse(trade, trade.cashBalanceUsdAfter());
	}

	private TradeExecutionResponse toTradeResponse(MockTradeLedgerEntry trade, BigDecimal cashBalanceUsd) {
		return new TradeExecutionResponse(
				trade.tradeId(),
				trade.accountId(),
				trade.stockCode(),
				trade.stockName(),
				trade.side(),
				trade.quantity(),
				moneyText(trade.executionPriceUsd()),
				moneyText(trade.grossAmountUsd()),
				moneyText(trade.realizedPnlUsd()),
				trade.remainingQuantity(),
				moneyText(trade.averagePriceUsdAfter()),
				moneyText(cashBalanceUsd),
				TRADING_MODE,
				trade.executedAt());
	}

	private PortfolioValuationHistoryItemResponse toPortfolioValuationHistoryItemResponse(
			PortfolioValuationSnapshot snapshot) {
		return new PortfolioValuationHistoryItemResponse(
				snapshot.snapshotId(),
				snapshot.currency(),
				moneyText(snapshot.cashBalanceUsd()),
				moneyText(snapshot.totalMarketValueUsd()),
				moneyText(snapshot.totalAssetValueUsd()),
				moneyText(snapshot.realizedPnlUsd()),
				moneyText(snapshot.unrealizedPnlUsd()),
				snapshot.holdingCount(),
				snapshot.valuedAt());
	}

	private TradeOrderPlacementResponse toOrderResponse(
			PendingLimitOrder order,
			TradeExecutionResponse tradeExecution,
			String message) {
		return new TradeOrderPlacementResponse(
				order.orderId(),
				order.accountId(),
				order.stockCode(),
				order.stockName(),
				order.side(),
				order.quantity(),
				TradeOrderType.LIMIT,
				moneyText(order.limitPriceUsd()),
				moneyText(order.observedPriceUsd()),
				order.status(),
				tradeExecution,
				TRADING_MODE,
				message,
				order.createdAt(),
				order.filledAt());
	}

	private void validateLimitOrderBalance(
			MockUsdAccount account,
			OmniLensMarketQuote quote,
			TradeSide side,
			long quantity,
			BigDecimal limitPriceUsd) {
		if (side == TradeSide.BUY) {
			BigDecimal maxGrossAmountUsd = money(limitPriceUsd.multiply(BigDecimal.valueOf(quantity)));
			if (account.cashBalanceUsd().compareTo(maxGrossAmountUsd) < 0) {
				throw new BusinessException(ErrorCode.MOCK_ACCOUNT_INSUFFICIENT_BALANCE);
			}
			return;
		}
		MockHolding holding = tradeRepository.findHolding(account.accountId(), quote.stockCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_HOLDING_INSUFFICIENT_QUANTITY));
		if (holding.quantity() < quantity) {
			throw new BusinessException(ErrorCode.MOCK_HOLDING_INSUFFICIENT_QUANTITY);
		}
	}

	private boolean limitReached(TradeSide side, BigDecimal observedPriceUsd, BigDecimal limitPriceUsd) {
		if (side == TradeSide.BUY) {
			return observedPriceUsd.compareTo(limitPriceUsd) <= 0;
		}
		return observedPriceUsd.compareTo(limitPriceUsd) >= 0;
	}

	private void ensureMarketOpen(Instant now) {
		if (!isMarketOpen(now)) {
			throw new BusinessException(ErrorCode.MARKET_CLOSED);
		}
	}

	private boolean isMarketOpen(Instant now) {
		ZonedDateTime koreanTime = now.atZone(KOREA_ZONE);
		DayOfWeek dayOfWeek = koreanTime.getDayOfWeek();
		if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
			return false;
		}
		LocalTime time = koreanTime.toLocalTime();
		return !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
	}

	private Instant now() {
		return Instant.now(clock);
	}

	private OmniLensMarketQuote quoteFromTick(MarketQuoteTickRequest request) {
		return new OmniLensMarketQuote(
				request.stockCode(),
				request.stockName(),
				request.stockNameEn(),
				request.market(),
				request.currentPriceKrw(),
				request.changeRate(),
				request.volume(),
				request.currentPriceKrw(),
				request.marketSession(),
				request.afterHoursPriceKrw(),
				request.afterHoursLocalCurrencyPrice(),
				request.afterHoursChangeRate(),
				request.afterHoursVolume(),
				request.afterHoursMarketDataTime(),
				"KRW",
				request.localCurrencyPrice(),
				request.localCurrency(),
				request.fxRate(),
				request.fxRateTime(),
				request.fxRateSource(),
				request.fxStale(),
				0L,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				null,
				request.marketDataTime(),
				request.source());
	}

	private String displayName(OmniLensMarketQuote quote) {
		return StockDisplayNameFormatter.displayName(quote.stockNameEn(), quote.stockName());
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private String moneyText(BigDecimal value) {
		return money(value).toPlainString();
	}

	private String rateText(BigDecimal pnl, BigDecimal costBasis) {
		if (costBasis.signum() == 0) {
			return "0.00";
		}
		return pnl.multiply(BigDecimal.valueOf(100))
				.divide(costBasis, 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private record ExecutionResult(MockTradeLedgerEntry trade, TradeExecutionResponse response) {
	}
}
