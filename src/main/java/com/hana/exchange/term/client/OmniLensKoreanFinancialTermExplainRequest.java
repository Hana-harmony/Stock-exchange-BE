package com.hana.exchange.term.client;

public record OmniLensKoreanFinancialTermExplainRequest(
		String term,
		String locale,
		String sourceType,
		String title,
		String context,
		String stockCode,
		String stockName,
		String articleId,
		String articleUrl,
		String userKey,
		String sessionKey,
		boolean allowWebSearch
) {
}
