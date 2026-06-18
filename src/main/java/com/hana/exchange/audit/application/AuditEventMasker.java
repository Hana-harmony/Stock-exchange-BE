package com.hana.exchange.audit.application;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AuditEventMasker {

	private static final Pattern EMAIL = Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
	private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?\\d{1,3}[-. ]?)?(?:\\d{2,4}[-. ]?){2}\\d{4}\\b");
	private static final Pattern KOREAN_RRN = Pattern.compile("\\b\\d{6}-?[1-4]\\d{6}\\b");
	private static final Pattern LONG_SECRET = Pattern.compile("\\b[A-Za-z0-9+/=_-]{24,}\\b");

	public String mask(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String masked = EMAIL.matcher(value).replaceAll("[MASKED_EMAIL]");
		masked = KOREAN_RRN.matcher(masked).replaceAll("[MASKED_RRN]");
		masked = PHONE.matcher(masked).replaceAll("[MASKED_PHONE]");
		return LONG_SECRET.matcher(masked).replaceAll("[MASKED_SECRET]");
	}
}
