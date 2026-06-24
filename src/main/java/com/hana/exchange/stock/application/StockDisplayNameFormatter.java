package com.hana.exchange.stock.application;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class StockDisplayNameFormatter {

	private static final Pattern PREFERRED_SUFFIX = Pattern.compile("(.+?)(\\d?우B?|우)$");
	private static final Map<String, String> EXACT_NAMES = exactNames();
	private static final Map<String, String> SUFFIX_NAMES = suffixNames();
	private static final String[] CHOSEONG = {
			"g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s",
			"ss", "", "j", "jj", "ch", "k", "t", "p", "h"
	};
	private static final String[] JUNGSEONG = {
			"a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa",
			"wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
	};
	private static final String[] JONGSEONG = {
			"", "k", "k", "ks", "n", "nj", "nh", "t", "l", "lk",
			"lm", "lb", "ls", "lt", "lp", "lh", "m", "p", "ps", "t",
			"t", "ng", "t", "t", "k", "t", "p", "h"
	};

	private StockDisplayNameFormatter() {
	}

	public static String displayName(String stockNameEn, String stockName) {
		return displayName(stockNameEn, stockName, stockName);
	}

	public static String displayName(String stockNameEn, String stockName, String fallback) {
		if (StringUtils.hasText(stockNameEn)
				&& StringUtils.hasText(stockName)
				&& !stockNameEn.equalsIgnoreCase(stockName)) {
			return stockNameEn + " (" + stockName + ")";
		}
		if (StringUtils.hasText(stockNameEn) && !containsHangul(stockNameEn)) {
			return stockNameEn;
		}
		if (StringUtils.hasText(stockName)) {
			String fallbackEnglishName = fallbackEnglishName(stockName);
			if (StringUtils.hasText(fallbackEnglishName)
					&& !fallbackEnglishName.equalsIgnoreCase(stockName)) {
				return fallbackEnglishName + " (" + stockName + ")";
			}
			return stockName;
		}
		if (StringUtils.hasText(stockNameEn)) {
			return stockNameEn;
		}
		return fallback;
	}

	private static String fallbackEnglishName(String stockName) {
		String trimmed = stockName.trim();
		if (!containsHangul(trimmed)) {
			return trimmed;
		}

		String preferredSuffix = "";
		Matcher matcher = PREFERRED_SUFFIX.matcher(trimmed);
		if (matcher.matches()) {
			trimmed = matcher.group(1);
			preferredSuffix = " " + preferredName(matcher.group(2));
		}

		String translated = translateMixedName(trimmed).replaceAll("\\s+", " ").trim();
		return (translated + preferredSuffix).replaceAll("\\s+", " ").trim();
	}

	private static String translateMixedName(String value) {
		StringBuilder result = new StringBuilder();
		StringBuilder token = new StringBuilder();
		Boolean hangulToken = null;
		for (int offset = 0; offset < value.length();) {
			int codePoint = value.codePointAt(offset);
			boolean hangul = isHangul(codePoint);
			boolean latinOrDigit = Character.isLetterOrDigit(codePoint) && !hangul;
			if (!hangul && !latinOrDigit) {
				flushToken(result, token, hangulToken);
				hangulToken = null;
				result.append(' ');
				offset += Character.charCount(codePoint);
				continue;
			}
			if (hangulToken != null && hangulToken != hangul) {
				flushToken(result, token, hangulToken);
				token.setLength(0);
			}
			hangulToken = hangul;
			token.appendCodePoint(codePoint);
			offset += Character.charCount(codePoint);
		}
		flushToken(result, token, hangulToken);
		return result.toString();
	}

	private static void flushToken(StringBuilder result, StringBuilder token, Boolean hangulToken) {
		if (token.isEmpty()) {
			return;
		}
		if (!result.isEmpty()) {
			result.append(' ');
		}
		String raw = token.toString();
		result.append(Boolean.TRUE.equals(hangulToken) ? translateHangulToken(raw) : raw);
		token.setLength(0);
	}

	private static String translateHangulToken(String token) {
		String exactName = EXACT_NAMES.get(token);
		if (exactName != null) {
			return exactName;
		}
		for (Map.Entry<String, String> entry : SUFFIX_NAMES.entrySet()) {
			String suffix = entry.getKey();
			if (token.equals(suffix)) {
				return entry.getValue();
			}
			if (token.endsWith(suffix)) {
				String prefix = token.substring(0, token.length() - suffix.length());
				if (StringUtils.hasText(prefix)) {
					return titleCase(romanize(prefix)) + " " + entry.getValue();
				}
			}
		}
		return titleCase(romanize(token));
	}

	private static String romanize(String value) {
		StringBuilder result = new StringBuilder();
		for (int offset = 0; offset < value.length();) {
			int codePoint = value.codePointAt(offset);
			if (!isHangul(codePoint)) {
				result.appendCodePoint(codePoint);
				offset += Character.charCount(codePoint);
				continue;
			}
			int syllable = codePoint - 0xAC00;
			int jong = syllable % 28;
			int jung = ((syllable - jong) / 28) % 21;
			int cho = ((syllable - jong) / 28) / 21;
			result.append(CHOSEONG[cho]).append(JUNGSEONG[jung]).append(JONGSEONG[jong]);
			offset += Character.charCount(codePoint);
		}
		return result.toString();
	}

	private static String titleCase(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		StringBuilder result = new StringBuilder();
		boolean nextUpper = true;
		for (int index = 0; index < lower.length(); index++) {
			char current = lower.charAt(index);
			if (!Character.isLetter(current)) {
				result.append(current);
				nextUpper = true;
				continue;
			}
			result.append(nextUpper ? Character.toUpperCase(current) : current);
			nextUpper = false;
		}
		return result.toString();
	}

	private static String preferredName(String value) {
		String order = value.replace("우", "").replace("B", "");
		String suffix = value.endsWith("B") ? "B" : "";
		String detail = (order + suffix).trim();
		return detail.isBlank() ? "Preferred" : "Preferred " + detail;
	}

	private static boolean containsHangul(String value) {
		return value.codePoints().anyMatch(StockDisplayNameFormatter::isHangul);
	}

	private static boolean isHangul(int codePoint) {
		return codePoint >= 0xAC00 && codePoint <= 0xD7A3;
	}

	private static Map<String, String> exactNames() {
		Map<String, String> names = new LinkedHashMap<>();
		names.put("삼성", "Samsung");
		names.put("현대", "Hyundai");
		names.put("기아", "Kia");
		names.put("하나", "Hana");
		names.put("신한", "Shinhan");
		names.put("우리", "Woori");
		names.put("카카오", "Kakao");
		names.put("네이버", "NAVER");
		names.put("셀트리온", "Celltrion");
		names.put("두산", "Doosan");
		names.put("엘지", "LG");
		return Collections.unmodifiableMap(new LinkedHashMap<>(names));
	}

	private static Map<String, String> suffixNames() {
		Map<String, String> names = new LinkedHashMap<>();
		names.put("네트웍스", "Networks");
		names.put("모터스", "Motors");
		names.put("오토넥스", "Autonex");
		names.put("바이오", "Bio");
		names.put("홀딩스", "Holdings");
		names.put("지주", "Holdings");
		names.put("제약", "Pharmaceutical");
		names.put("화재", "Fire & Marine Insurance");
		names.put("생명", "Life Insurance");
		names.put("증권", "Securities");
		names.put("건설", "Engineering & Construction");
		names.put("전선", "Cable");
		names.put("고속", "Express");
		names.put("강업", "Steel");
		names.put("전자", "Electronics");
		names.put("전기", "Electric");
		names.put("금융", "Financial");
		names.put("에너지", "Energy");
		names.put("텔레콤", "Telecom");
		names.put("화학", "Chemical");
		names.put("은행", "Bank");
		names.put("게임즈", "Games");
		names.put("페이", "Pay");
		return Collections.unmodifiableMap(new LinkedHashMap<>(names));
	}
}
