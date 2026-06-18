package com.hana.exchange.trade.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.market.client.OmniLensOrderabilityClient;
import com.hana.exchange.market.client.OmniLensOrderabilityResponse;
import com.hana.exchange.trade.domain.TradeOrderabilityResponse;
import com.hana.exchange.trade.domain.TradeSide;

@Service
public class TradeOrderabilityService {

	private static final String TRADING_MODE = "EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING";
	private static final String UPPER_LIMIT = "UPPER_LIMIT";
	private static final String LOWER_LIMIT = "LOWER_LIMIT";

	private final AccountRepository accountRepository;
	private final OmniLensOrderabilityClient orderabilityClient;

	public TradeOrderabilityService(
			AccountRepository accountRepository,
			OmniLensOrderabilityClient orderabilityClient) {
		this.accountRepository = accountRepository;
		this.orderabilityClient = orderabilityClient;
	}

	public TradeOrderabilityResponse check(String accountId, String stockCode, TradeSide side, long quantity) {
		MockUsdAccount account = accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
		OmniLensOrderabilityResponse orderability = orderabilityClient.checkOrderability(stockCode, side, quantity);
		List<String> blockingReasons = blockingReasons(orderability);
		List<String> warnings = warnings(orderability, side);
		return new TradeOrderabilityResponse(
				account.accountId(),
				orderability.stockCode(),
				orderability.market(),
				side,
				quantity,
				blockingReasons.isEmpty(),
				blockingReasons,
				warnings,
				orderability.source(),
				TRADING_MODE,
				orderability.checkedAt());
	}

	private List<String> blockingReasons(OmniLensOrderabilityResponse orderability) {
		List<String> reasons = new ArrayList<>();
		if (!orderability.orderable() && hasText(orderability.orderBlockedReason())) {
			reasons.add(orderability.orderBlockedReason());
		}
		if (orderability.tradingHalted()) {
			reasons.add("TRADING_HALTED");
		}
		if (orderability.foreignLimitExceeded()) {
			reasons.add("FOREIGN_LIMIT_EXCEEDED");
		}
		if (!orderability.orderable() && reasons.isEmpty()) {
			reasons.add("ORDER_NOT_ALLOWED");
		}
		return List.copyOf(reasons);
	}

	private List<String> warnings(OmniLensOrderabilityResponse orderability, TradeSide side) {
		List<String> warnings = new ArrayList<>();
		if (orderability.viActive()) {
			warnings.add("VI_ACTIVE");
		}
		if (side == TradeSide.BUY && UPPER_LIMIT.equals(orderability.priceLimitState())) {
			warnings.add("BUY_AT_UPPER_LIMIT");
		}
		if (side == TradeSide.SELL && LOWER_LIMIT.equals(orderability.priceLimitState())) {
			warnings.add("SELL_AT_LOWER_LIMIT");
		}
		return List.copyOf(warnings);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
