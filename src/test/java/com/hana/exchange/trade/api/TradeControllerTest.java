package com.hana.exchange.trade.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.client.OmniLensOrderabilityClient;
import com.hana.exchange.market.client.OmniLensOrderabilityResponse;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
@AutoConfigureMockMvc
class TradeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@MockitoBean
	private OmniLensOrderabilityClient omniLensOrderabilityClient;

	@Test
	void buyUsesOmniLensUsdQuoteAndUpdatesPortfolio() throws Exception {
		String accountId = fundedAccount("BuyTrader01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "BUY",
								  "quantity": 2
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.stockName").value("Samsung Electronics"))
				.andExpect(jsonPath("$.data.side").value("BUY"))
				.andExpect(jsonPath("$.data.quantity").value(2))
				.andExpect(jsonPath("$.data.executionPriceUsd").value("50.00"))
				.andExpect(jsonPath("$.data.grossAmountUsd").value("100.00"))
				.andExpect(jsonPath("$.data.cashBalanceUsdAfter").value("100.00"))
				.andExpect(jsonPath("$.data.remainingQuantity").value(2))
				.andExpect(jsonPath("$.data.tradingMode").value("EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING"));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/portfolio", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("100.00"))
				.andExpect(jsonPath("$.data.realizedPnlUsd").value("0.00"))
				.andExpect(jsonPath("$.data.holdings[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.holdings[0].quantity").value(2))
				.andExpect(jsonPath("$.data.holdings[0].averagePriceUsd").value("50.00"))
				.andExpect(jsonPath("$.data.holdings[0].costBasisUsd").value("100.00"));
	}

	@Test
	void sellCalculatesRealizedPnlWithoutRealOrderExecution() throws Exception {
		String accountId = fundedAccount("SellTrader01", "300.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"))
				.thenReturn(quote("005930", "Samsung Electronics", "60.00"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "BUY",
								  "quantity": 3
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "SELL",
								  "quantity": 1
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.side").value("SELL"))
				.andExpect(jsonPath("$.data.executionPriceUsd").value("60.00"))
				.andExpect(jsonPath("$.data.grossAmountUsd").value("60.00"))
				.andExpect(jsonPath("$.data.realizedPnlUsd").value("10.00"))
				.andExpect(jsonPath("$.data.remainingQuantity").value(2))
				.andExpect(jsonPath("$.data.cashBalanceUsdAfter").value("210.00"));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/portfolio", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.cashBalanceUsd").value("210.00"))
				.andExpect(jsonPath("$.data.realizedPnlUsd").value("10.00"))
				.andExpect(jsonPath("$.data.holdings[0].quantity").value(2))
				.andExpect(jsonPath("$.data.holdings[0].averagePriceUsd").value("50.00"));
	}

	@Test
	void buyRejectsInsufficientMockUsdBalance() throws Exception {
		String accountId = fundedAccount("PoorTrader01", "10.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "BUY",
								  "quantity": 1
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("TRADE_001"));
	}

	@Test
	void sellRejectsInsufficientMockHoldingQuantity() throws Exception {
		String accountId = fundedAccount("NoHoldingTrader01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "005930",
								  "side": "SELL",
								  "quantity": 1
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("TRADE_002"));
	}

	@Test
	void tradeApiRejectsInvalidInput() throws Exception {
		String accountId = fundedAccount("InvalidTradeTrader01", "200.00");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "ABCDEF",
								  "side": "BUY",
								  "quantity": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	@Test
	void orderabilityWarnsWhenViAndUpperLimitAreActive() throws Exception {
		String accountId = fundedAccount("OrderabilityTrader01", "200.00");
		when(omniLensOrderabilityClient.checkOrderability("005930", TradeSide.BUY, 2))
				.thenReturn(orderability("005930", true, null, false, true, "UPPER_LIMIT", false));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/trades/orderability", accountId)
						.param("stockCode", "005930")
						.param("side", "BUY")
						.param("quantity", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.stockCode").value("005930"))
				.andExpect(jsonPath("$.data.side").value("BUY"))
				.andExpect(jsonPath("$.data.quantity").value(2))
				.andExpect(jsonPath("$.data.canPlaceMockOrder").value(true))
				.andExpect(jsonPath("$.data.warnings[0]").value("VI_ACTIVE"))
				.andExpect(jsonPath("$.data.warnings[1]").value("BUY_AT_UPPER_LIMIT"))
				.andExpect(jsonPath("$.data.tradingMode").value("EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING"));
	}

	@Test
	void orderabilityBlocksWhenForeignLimitExceeded() throws Exception {
		String accountId = fundedAccount("OrderBlockTrader01", "200.00");
		when(omniLensOrderabilityClient.checkOrderability("005930", TradeSide.BUY, 1))
				.thenReturn(orderability("005930", false, "FOREIGN_LIMIT_EXCEEDED", true, false, "NORMAL", false));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/trades/orderability", accountId)
						.param("stockCode", "005930")
						.param("side", "BUY")
						.param("quantity", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.canPlaceMockOrder").value(false))
				.andExpect(jsonPath("$.data.blockingReasons[0]").value("FOREIGN_LIMIT_EXCEEDED"));
	}

	@Test
	void orderabilityRejectsInvalidInput() throws Exception {
		String accountId = fundedAccount("OrderInvalidTrader01", "200.00");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/trades/orderability", accountId)
						.param("stockCode", "ABCDEF")
						.param("side", "BUY")
						.param("quantity", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private String fundedAccount(String username, String amountUsd) throws Exception {
		String accountId = signUpAndGetAccountId(username);
		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": %s
								}
								""".formatted(amountUsd)))
				.andExpect(status().isOk());
		return accountId;
	}

	private String signUpAndGetAccountId(String username) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "localPass123!"
								}
								""".formatted(username)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accountId");
	}

	private OmniLensMarketQuote quote(String stockCode, String stockNameEn, String usdPrice) {
		return new OmniLensMarketQuote(
				stockCode,
				"삼성전자",
				stockNameEn,
				"KOSPI",
				new BigDecimal("75000"),
				new BigDecimal("1.25"),
				1000000L,
				new BigDecimal("75000"),
				"KRW",
				new BigDecimal(usdPrice),
				"USD",
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}

	private OmniLensOrderabilityResponse orderability(
			String stockCode,
			boolean orderable,
			String orderBlockedReason,
			boolean foreignLimitExceeded,
			boolean viActive,
			String priceLimitState,
			boolean tradingHalted) {
		return new OmniLensOrderabilityResponse(
				stockCode,
				"KOSPI",
				orderable,
				orderBlockedReason,
				foreignLimitExceeded,
				viActive,
				priceLimitState,
				tradingHalted,
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}
}
