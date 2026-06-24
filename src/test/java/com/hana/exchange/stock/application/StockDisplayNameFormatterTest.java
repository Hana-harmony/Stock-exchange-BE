package com.hana.exchange.stock.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StockDisplayNameFormatterTest {

	@Test
	void displayNameKeepsAuthoritativeEnglishNameWithKoreanName() {
		assertThat(StockDisplayNameFormatter.displayName("Samsung Electronics", "삼성전자"))
				.isEqualTo("Samsung Electronics (삼성전자)");
	}

	@Test
	void displayNameBuildsEnglishFallbackWhenKisMasterHasOnlyKoreanName() {
		assertThat(StockDisplayNameFormatter.displayName("경방", "경방"))
				.isEqualTo("Gyeongbang (경방)");
		assertThat(StockDisplayNameFormatter.displayName("삼양홀딩스", "삼양홀딩스"))
				.isEqualTo("Samyang Holdings (삼양홀딩스)");
		assertThat(StockDisplayNameFormatter.displayName("하이트진로2우B", "하이트진로2우B"))
				.isEqualTo("Haiteujinro Preferred 2B (하이트진로2우B)");
	}

	@Test
	void displayNamePreservesLatinTickerNameWithoutKoreanSuffix() {
		assertThat(StockDisplayNameFormatter.displayName("NAVER", "NAVER"))
				.isEqualTo("NAVER");
		assertThat(StockDisplayNameFormatter.displayName("KR모터스", "KR모터스"))
				.isEqualTo("KR Motors (KR모터스)");
	}
}
