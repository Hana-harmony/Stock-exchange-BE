package com.hana.exchange.notification.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertTargetResponse;
import com.hana.exchange.audit.application.AuditEventService;
import com.hana.exchange.audit.domain.AuditEventType;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.notification.domain.NotificationInboxResponse;
import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;
import com.hana.exchange.notification.domain.NotificationItemResponse;
import com.hana.exchange.tax.domain.TaxRefundCase;

@Service
public class NotificationService {

	private final AccountRepository accountRepository;
	private final NotificationRepository notificationRepository;
	private final IdGenerator idGenerator;
	private final PushNotificationSender pushNotificationSender;
	private final AuditEventService auditEventService;

	public NotificationService(
			AccountRepository accountRepository,
			NotificationRepository notificationRepository,
			IdGenerator idGenerator,
			PushNotificationSender pushNotificationSender,
			AuditEventService auditEventService) {
		this.accountRepository = accountRepository;
		this.notificationRepository = notificationRepository;
		this.idGenerator = idGenerator;
		this.pushNotificationSender = pushNotificationSender;
		this.auditEventService = auditEventService;
	}

	public void storeAlertNotifications(AlertEvent event, List<AlertTargetResponse> targets) {
		for (AlertTargetResponse target : targets) {
			if (notificationRepository.existsForEventAndAccount(event.eventId(), target.accountId())) {
				continue;
			}
			NotificationItem pendingNotification = new NotificationItem(
					idGenerator.newNotificationId(),
					target.accountId(),
					target.userId(),
					event.eventId(),
					"ALERT_EVENT",
					event.eventId(),
					event.sourceType(),
					event.title(),
					event.summary(),
					event.originalUrl(),
					event.stockCode(),
					event.matchingStockCodes(),
					target.matchReasons(),
					NotificationDeliveryStatus.PENDING,
					null,
					0,
					null,
					null,
					false,
					Instant.now(),
					null);
			NotificationDeliveryResult deliveryResult = sendSafely(pendingNotification);
			notificationRepository.save(pendingNotification.markDelivery(deliveryResult));
		}
	}

	public void storeTaxRecaptureRiskNotification(TaxRefundCase taxCase) {
		if (notificationRepository.existsForSubjectAndAccount("TAX_REFUND_CASE", taxCase.caseId(), taxCase.accountId())) {
			return;
		}
		NotificationItem pendingNotification = new NotificationItem(
				idGenerator.newNotificationId(),
				taxCase.accountId(),
				taxCase.userId(),
				null,
				"TAX_REFUND_CASE",
				taxCase.caseId(),
				"TAX_RECAPTURE_RISK",
				"Tax refund recapture risk",
				"Your tax refund case for " + taxCase.taxYear()
						+ " has been flagged for post-payment recapture review.",
				"",
				null,
				List.of(),
				List.of("TAX_RECAPTURE_RISK"),
				NotificationDeliveryStatus.PENDING,
				null,
				0,
				null,
				null,
				false,
				Instant.now(),
				null);
		NotificationDeliveryResult deliveryResult = sendSafely(pendingNotification);
		notificationRepository.save(pendingNotification.markDelivery(deliveryResult));
	}

	public int dispatchRetryablePushNotifications(int batchSize, int maxAttemptCount) {
		List<NotificationItem> retryableItems = notificationRepository.findRetryableForDelivery(batchSize, maxAttemptCount);
		for (NotificationItem item : retryableItems) {
			notificationRepository.save(item.markDelivery(sendSafely(item)));
		}
		return retryableItems.size();
	}

	public NotificationInboxResponse getInbox(String accountId) {
		assertAccount(accountId);
		List<NotificationItemResponse> items = notificationRepository.findByAccountId(accountId)
				.stream()
				.map(this::toResponse)
				.toList();
		long unreadCount = items.stream()
				.filter(item -> !item.read())
				.count();
		return new NotificationInboxResponse(accountId, (int) unreadCount, items.size(), items, Instant.now());
	}

	public NotificationItemResponse markRead(String accountId, String notificationId) {
		assertAccount(accountId);
		NotificationItem item = notificationRepository.findByAccountIdAndNotificationId(accountId, notificationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		NotificationItem readItem = item.markRead(Instant.now());
		notificationRepository.save(readItem);
		auditEventService.record(
				readItem.accountId(),
				readItem.userId(),
				AuditEventType.NOTIFICATION_READ,
				"NOTIFICATION",
				readItem.notificationId(),
				"Read notification for event " + readItem.eventId(),
				readItem.readAt());
		return toResponse(readItem);
	}

	private void assertAccount(String accountId) {
		accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
	}

	private NotificationItemResponse toResponse(NotificationItem item) {
		return new NotificationItemResponse(
				item.notificationId(),
				item.eventId(),
				item.subjectType(),
				item.subjectId(),
				item.sourceType(),
				item.title(),
				item.summary(),
				item.originalUrl(),
				item.primaryStockCode(),
				item.matchedStockCodes(),
				item.matchReasons(),
				item.deliveryStatus(),
				item.deliveryProvider(),
				item.deliveryAttemptCount(),
				item.deliveredAt(),
				item.lastDeliveryError(),
				item.read(),
				item.createdAt(),
				item.readAt());
	}

	private NotificationDeliveryResult sendSafely(NotificationItem notification) {
		try {
			return pushNotificationSender.send(notification);
		} catch (RuntimeException exception) {
			return NotificationDeliveryResult.failed("PUSH_PROVIDER", exception.getMessage());
		}
	}
}
