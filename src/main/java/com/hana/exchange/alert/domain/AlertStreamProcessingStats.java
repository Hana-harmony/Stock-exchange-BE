package com.hana.exchange.alert.domain;

import java.time.Instant;

public record AlertStreamProcessingStats(
		long acceptedCount,
		long ingestedCount,
		long rejectedCount,
		long droppedCount,
		long retryScheduledCount,
		long failedIngestCount,
		int bufferDepth,
		Instant lastPublishedAt
) {
}
