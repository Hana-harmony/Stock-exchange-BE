package com.hana.exchange.trade.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
import com.hana.exchange.trade.domain.HoldingResponse;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.PortfolioResponse;
import com.hana.exchange.trade.domain.TradeExecutionResponse;
import com.hana.exchange.trade.domain.TradeOrderRequest;
import com.hana.exchange.trade.domain.TradeSide;

@Service
public class TradeService {

	private static final String USD = "USD";
	private static final String TRADING_MODE = "EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING";
	private static final int RECENT_TRADE_LIMIT = 20;

	private final AccountRepository accountRepository;
	private final TradeRepository tradeRepository;
	private final OmniLensMarketQuoteClient quoteClient;
	private final TradeOrderabilityService tradeOrderabilityService;
	private final IdGenerator idGenerator;
	private final AuditEventService auditEventService;

	public TradeService(
			AccountRepository accountRepository,
			TradeRepository tradeRepository,
			OmniLensMarketQuoteClient quoteClient,
			TradeOrderabilityService tradeOrderabilityService,
			IdGenerator idGenerator,
			AuditEventService auditEventService) {
		this.accountRepository = accountRepository;
		this.tradeRepository = tradeRepository;
		this.quoteClient = quoteClient;
		this.tradeOrderabilityService = tradeOrderabilityService;
		this.idGenerator = idGenerator;
		this.auditEventService = auditEventService;
	}

	public TradeExecutionResponse execute(String accountId, TradeOrderRequest request) {
		if (!tradeOrderabilityService.check(accountId, request.stockCode(), request.side(), request.quantity()).canPlaceMockOrder()) {
			throw new BusinessException(ErrorCode.MOCK_ORDER_BLOCKED);
		}
		MockUsdAccount account = account(accountId);
		OmniLensMarketQuote quote = quoteClient.getQuote(request.stockCode(), USD);
		BigDecimal executionPriceUsd = money(quote.localCurrencyPrice());
		BigDecimal grossAmountUsd = money(executionPriceUsd.multiply(BigDecimal.valueOf(request.quantity())));
		Instant now = Instant.now();

		if (request.side() == TradeSide.BUY) {
			return buy(account, quote, request.quantity(), executionPriceUsd, grossAmountUsd, now);
		}
		return sell(account, quote, request.quantity(), executionPriceUsd, grossAmountUsd, now);
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
		return new PortfolioResponse(
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
				Instant.now());
	}

	private TradeExecutionResponse buy(
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
		return toTradeResponse(trade, updatedAccount.cashBalanceUsd());
	}

	private TradeExecutionResponse sell(
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
		return toTradeResponse(trade, updatedAccount.cashBalanceUsd());
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

	private String displayName(OmniLensMarketQuote quote) {
		if (quote.stockNameEn() != null && !quote.stockNameEn().isBlank()) {
			return quote.stockNameEn();
		}
		return quote.stockName();
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
}
