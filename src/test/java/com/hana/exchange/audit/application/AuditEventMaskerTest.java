package com.hana.exchange.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditEventMaskerTest {

	private final AuditEventMasker auditEventMasker = new AuditEventMasker();

	@Test
	void masksPersonalInformationAndSecretsInAuditText() {
		String masked = auditEventMasker.mask(
				"user=test@example.com phone=010-1234-5678 rrn=900101-1234567 token=abcdefghijklmnopqrstuvwxyz123456");

		assertThat(masked).contains("[MASKED_EMAIL]");
		assertThat(masked).contains("[MASKED_PHONE]");
		assertThat(masked).contains("[MASKED_RRN]");
		assertThat(masked).contains("[MASKED_SECRET]");
		assertThat(masked).doesNotContain("test@example.com", "010-1234-5678", "900101-1234567");
	}
}
