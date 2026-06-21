package com.hana.exchange.tax.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.client.OmniLensOrderabilityClient;
import com.hana.exchange.market.client.OmniLensOrderabilityResponse;
import com.hana.exchange.support.AuthTestSupport;
import com.hana.exchange.support.AuthTestSupport.AuthSession;
import com.hana.exchange.tax.client.OmniLensTaxStatusClient;
import com.hana.exchange.tax.client.OmniLensTaxStatusSyncRequest;
import com.hana.exchange.tax.client.OmniLensTaxStatusSyncResponse;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
@AutoConfigureMockMvc
class TaxRefundCaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	@MockitoBean
	private OmniLensOrderabilityClient omniLensOrderabilityClient;

	@MockitoBean
	private OmniLensTaxStatusClient omniLensTaxStatusClient;

	@MockitoBean
	private Clock clock;

	@BeforeEach
	void prepareTradingSession() {
		when(clock.instant()).thenReturn(Instant.parse("2026-06-18T01:00:00Z"));
		when(omniLensOrderabilityClient.checkOrderability(anyString(), any(TradeSide.class), anyLong()))
				.thenAnswer(invocation -> orderability(invocation.getArgument(0)));
	}

	@Test
	void createRefundCaseMatchesSellRealizedPnlAndEstimatesRefund() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		AuthSession session = fundedAccount("TaxTrader01", "200.00");
		when(omniLensMarketQuoteClient.getQuote("005930", "USD"))
				.thenReturn(quote("005930", "Samsung Electronics", "50.00"))
				.thenReturn(quote("005930", "Samsung Electronics", "70.00"));
		buy(session, "005930", 2);
		sell(session, "005930", 1);

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(taxYear, true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.caseId").isNotEmpty())
				.andExpect(jsonPath("$.data.accountId").value(session.accountId()))
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

		mockMvc.perform(get("/api/v1/accounts/{accountId}/tax/refund-status", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("READY_FOR_HANA_SYNC"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("1.40"))
				.andExpect(jsonPath("$.data.matchedTradeCount").value(1));
	}

	@Test
	void uploadTaxDocumentsAndAttachThemToRefundCase() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxDocUpload01");
		String residenceDocumentId = uploadDocument(
				session,
				"RESIDENCE_CERTIFICATE",
				"residence.pdf",
				"residence certificate".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		String reducedTaxDocumentId = uploadDocument(
				session,
				"REDUCED_TAX_APPLICATION",
				"reduced-tax.pdf",
				"reduced tax application".getBytes(java.nio.charset.StandardCharsets.UTF_8));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "taxYear": %d,
								  "treatyCountry": "US",
								  "residenceCertificateFileName": "residence.pdf",
								  "reducedTaxApplicationFileName": "reduced-tax.pdf",
								  "residenceCertificateDocumentId": "%s",
								  "reducedTaxApplicationDocumentId": "%s",
								  "advancePaymentRequested": true
								}
								""".formatted(taxYear, residenceDocumentId, reducedTaxDocumentId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.residenceCertificateDocumentId").value(residenceDocumentId))
				.andExpect(jsonPath("$.data.reducedTaxApplicationDocumentId").value(reducedTaxDocumentId));
	}

	@Test
	void createRefundCaseRejectsDocumentOwnedByAnotherAccount() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		AuthSession owner = AuthTestSupport.signUpAndLogin(mockMvc, "TaxDocOwner01");
		AuthSession requester = AuthTestSupport.signUpAndLogin(mockMvc, "TaxDocRequester01");
		String residenceDocumentId = uploadDocument(
				owner,
				"RESIDENCE_CERTIFICATE",
				"owner-residence.pdf",
				"owner residence certificate".getBytes(java.nio.charset.StandardCharsets.UTF_8));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", requester.accountId())
						.header(HttpHeaders.AUTHORIZATION, requester.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "taxYear": %d,
								  "treatyCountry": "US",
								  "residenceCertificateFileName": "owner-residence.pdf",
								  "reducedTaxApplicationFileName": "reduced-tax.pdf",
								  "residenceCertificateDocumentId": "%s",
								  "advancePaymentRequested": true
								}
								""".formatted(taxYear, residenceDocumentId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TAX_003"));
	}

	@Test
	void refundStatusReturnsNotSubmittedBeforeTaxCaseExists() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxEmpty01");

		mockMvc.perform(get("/api/v1/accounts/{accountId}/tax/refund-status", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accountId").value(session.accountId()))
				.andExpect(jsonPath("$.data.status").value("NOT_SUBMITTED"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("0.00"))
				.andExpect(jsonPath("$.data.matchedTradeCount").value(0));
	}

	@Test
	void createRefundCaseWithoutProfitableSellTradeMarksNoRefundableProfit() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxNoProfit01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(taxYear, false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("NO_REFUNDABLE_PROFIT"))
				.andExpect(jsonPath("$.data.estimatedRefundUsd").value("0.00"))
				.andExpect(jsonPath("$.data.advancePaymentEligible").value(false));
	}

	@Test
	void syncRefundStatusCallsHanaAndUpdatesLatestTaxCaseStatus() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxSync01");
		when(omniLensTaxStatusClient.sync(any(OmniLensTaxStatusSyncRequest.class)))
				.thenReturn(new OmniLensTaxStatusSyncResponse(
						"TAX-SYNC-REMOTE",
						"SYNCED_WITH_HANA",
						Instant.parse("2026-06-18T06:30:00Z"),
						"HANA_OMNILENS_API"));
		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(taxYear, false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("NO_REFUNDABLE_PROFIT"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-status/sync", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("SYNCED_WITH_HANA"))
				.andExpect(jsonPath("$.data.updatedAt").value("2026-06-18T06:30:00Z"));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/tax/refund-status", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("SYNCED_WITH_HANA"));
		verify(omniLensTaxStatusClient).sync(any(OmniLensTaxStatusSyncRequest.class));
	}

	@Test
	void syncRefundStatusStoresRecaptureRiskNotificationOnce() throws Exception {
		int taxYear = Year.now(ZoneOffset.UTC).getValue();
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxRiskNotify01");
		when(omniLensTaxStatusClient.sync(any(OmniLensTaxStatusSyncRequest.class)))
				.thenReturn(new OmniLensTaxStatusSyncResponse(
						"TAX-RISK-REMOTE",
						"RECAPTURE_RISK",
						Instant.parse("2026-06-18T07:30:00Z"),
						"HANA_OMNILENS_API"));
		MvcResult createdCase = mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content(refundCasePayload(taxYear, false)))
				.andExpect(status().isOk())
				.andReturn();
		String caseId = JsonPath.read(createdCase.getResponse().getContentAsString(), "$.data.caseId");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-status/sync", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RECAPTURE_RISK"));

		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadCount").value(1))
				.andExpect(jsonPath("$.data.totalCount").value(1))
				.andExpect(jsonPath("$.data.notifications[0].eventId").doesNotExist())
				.andExpect(jsonPath("$.data.notifications[0].subjectType").value("TAX_REFUND_CASE"))
				.andExpect(jsonPath("$.data.notifications[0].subjectId").value(caseId))
				.andExpect(jsonPath("$.data.notifications[0].sourceType").value("TAX_RECAPTURE_RISK"))
				.andExpect(jsonPath("$.data.notifications[0].matchReasons[0]").value("TAX_RECAPTURE_RISK"))
				.andExpect(jsonPath("$.data.notifications[0].deliveryStatus").value("DELIVERED"))
				.andExpect(jsonPath("$.data.notifications[0].deliveryProvider").value("LOCAL_NOOP_PUSH"));

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-status/sync", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RECAPTURE_RISK"));
		mockMvc.perform(get("/api/v1/accounts/{accountId}/notifications", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").value(1));
	}

	@Test
	void syncRefundStatusRejectsWhenTaxCaseDoesNotExist() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxSyncMissing01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-status/sync", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("TAX_001"));
	}

	@Test
	void createRefundCaseRejectsInvalidPayload() throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, "TaxInvalid01");

		mockMvc.perform(post("/api/v1/accounts/{accountId}/tax/refund-cases", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
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

	private void buy(AuthSession session, String stockCode, long quantity) throws Exception {
		trade(session, stockCode, "BUY", quantity);
	}

	private void sell(AuthSession session, String stockCode, long quantity) throws Exception {
		trade(session, stockCode, "SELL", quantity);
	}

	private void trade(AuthSession session, String stockCode, String side, long quantity) throws Exception {
		mockMvc.perform(post("/api/v1/accounts/{accountId}/trades", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "stockCode": "%s",
								  "side": "%s",
								  "quantity": %d,
								  "orderType": "LIMIT",
								  "limitPriceUsd": %s
								}
								""".formatted(stockCode, side, quantity, side.equals("BUY") ? "50.00" : "70.00")))
				.andExpect(status().isOk());
	}

	private AuthSession fundedAccount(String username, String amountUsd) throws Exception {
		AuthSession session = AuthTestSupport.signUpAndLogin(mockMvc, username);
		mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", session.accountId())
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amountUsd": %s
								}
								""".formatted(amountUsd)))
				.andExpect(status().isOk());
		return session;
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

	private String uploadDocument(
			AuthSession session,
			String documentType,
			String fileName,
			byte[] content) throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				fileName,
				"application/pdf",
				content);
		MvcResult result = mockMvc.perform(multipart("/api/v1/accounts/{accountId}/tax/documents", session.accountId())
						.file(file)
						.param("documentType", documentType)
						.header(HttpHeaders.AUTHORIZATION, session.authorizationHeader()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.documentId").isNotEmpty())
				.andExpect(jsonPath("$.data.documentType").value(documentType))
				.andExpect(jsonPath("$.data.originalFileName").value(fileName))
				.andExpect(jsonPath("$.data.sizeBytes").value(content.length))
				.andExpect(jsonPath("$.data.storageKey").isNotEmpty())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data.documentId");
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

	private OmniLensOrderabilityResponse orderability(String stockCode) {
		return new OmniLensOrderabilityResponse(
				stockCode,
				"KOSPI",
				true,
				null,
				false,
				false,
				"NORMAL",
				false,
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}
}
