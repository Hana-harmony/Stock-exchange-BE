package com.hana.exchange.audit.application;

import java.util.List;

import com.hana.exchange.audit.domain.AuditEvent;

public interface AuditEventRepository {

	void save(AuditEvent event);

	List<AuditEvent> findByAccountId(String accountId, int limit);
}
