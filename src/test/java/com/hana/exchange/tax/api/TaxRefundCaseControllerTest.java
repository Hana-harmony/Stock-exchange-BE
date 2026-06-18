package com.hana.exchange.tax.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;

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

@SpringBootTest
@AutoConfigureMockMvc
class TaxRefundCaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@Test
	void createRefundCaseMatchesSellRealizedPnlAndEstimatesRefund() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		String accountId = fundedAccount("TaxTrader01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"))
				.thenReturn(quote("005930", "Samsung Electronics", "70.00"));
		buy(accountId, "005930", 2);
		sell(accountId, "005930", 1);

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(taxYear, true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.caseId").isNotEmpty())
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.taxYear").value(taxYear))
				.andExpect(jsonPath("$.data.treatyCountry").value("US"))
				.andExpect(jsonPath("$.data.status").value("READY_FOR_HANA_SYNC"))
				.andExpect(jsonPath("$.data.currency").value("USD"))
				.andExpect(jsonPath("$.data.totalSellAmountUsd").value("70.00"))
				.andExpect(jsonPath("$.data.realizedProfitUsd").value("20.00"))
				.andExpect(jsonPath("$.data.realizedLossUsd").value("0.00"))
				.andExpect(jsonPath("$.data.netRealizedPnlUsd").value("20.00"))
				.andExpect(jsonPath("$.data.taxableRealizedPnlUsd").value("20.00"))
				.andExpect(jsonPath("$.data.estimatedWithholdingTaxUsd").value("4.40"))
				.andExpect(jsonPath("$.data.estimatedTreatyTaxUsd").value("3.00"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("1.40"))
				.andExpect(jsonPath("$.data.advancePaymentEligible").value(true))
				.andExpect(jsonPath("$.data.matchedTradeCount").value(1))
				.andExpect(jsonPath("$.data.matchedTrades[0].stockCode").value("005930"))
				.andExpect(jsonPath("$.data.matchedTrades[0].realizedPnlUsd").value("20.00"))
				.andExpect(jsonPath("$.data.dataSource").value("EXCHANGE_MOCK_LEDGER_REALIZED_PNL"));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/tax/refund-status", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("READY_FOR_HANA_SYNC"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("1.40"))
				.andExpect(jsonPath("$.data.matchedTradeCount").value(1));
	}

	@Test
	void refundStatusReturnsNotSubmittedBeforeTaxCaseExists() throws Exception {
		String accountId = signUpAndGetAccountId("TaxEmpty01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/tax/refund-status", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accountId").value(accountId))
				.andExpect(jsonPath("$.data.status").value("NOT_SUBMITTED"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("0.00"))
				.andExpect(jsonPath("$.data.matchedTradeCount").value(0));
	}

	@Test
	void createRefundCaseWithoutProfitableSellTradeMarksNoRefundableProfit() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		String accountId = signUpAndGetAccountId("TaxNoProfit01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(taxYear, false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("NO_REFUNDABLE_PROFIT"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("0.00"))
				.andExpect(jsonPath("$.data.advancePaymentEligible").value(false));
	}

	@Test
	void createRefundCaseRejectsInvalidPayload() throws Exception {
		String accountId = signUpAndGetAccountId("TaxInvalid01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "taxYear": 2019,
								  "treatyCountry": "USA",
								  "residenceCertificateFileName": "",
								  "reducedTaxApplicationFileName": "",
								  "advancePaymentRequested": true
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_002"));
	}

	private void buy(String accountId, String stockCode, long quantity) throws Exception {
		trade(accountId, stockCode, "BUY", quantity);
	}

	private void sell(String accountId, String stockCode, long quantity) throws Exception {
		trade(accountId, stockCode, "SELL", quantity);
	}

	private void trade(String accountId, String stockCode, String side, long quantity) throws Exception {
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", accountId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "%s",
								  "side": "%s",
								  "quantity": %d
								}
								""".formatted(stockCode, side, quantity)))
				.andExpect(status().isOk());
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

	private String refundCasePayload(int taxYear, boolean advancePaymentRequested) {
		return """
				{
				  "taxYear": %d,
				  "treatyCountry": "US",
				  "residenceCertificateFileName": "residence-certificate.pdf",
				  "reducedTaxApplicationFileName": "reduced-tax-application.pdf",
				  "advancePaymentRequested": %s
				}
				""".formatted(taxYear, advancePaymentRequested);
	}

	private OmniLensMarketQuote quote(String stockCode, String stockNameEn, String usdPrice) {
		return new OmniLensMarketQuote(
				stockCode,
				"종목명",
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
}
