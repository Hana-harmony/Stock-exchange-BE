package com.hana.exchange.notification.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertTargetResponse;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.notification.domain.NotificationInboxResponse;
import com.hana.exchange.notification.domain.NotificationItem;
import com.hana.exchange.notification.domain.NotificationItemResponse;

@Service
public class NotificationService {

	private final AccountRepository accountRepository;
	private final NotificationRepository notificationRepository;
	private final IdGenerator idGenerator;

	public NotificationService(
			AccountRepository accountRepository,
			NotificationRepository notificationRepository,
			IdGenerator idGenerator) {
		this.accountRepository = accountRepository;
		this.notificationRepository = notificationRepository;
		this.idGenerator = idGenerator;
	}

	public void storeAlertNotifications(AlertEvent event, List<AlertTargetResponse> targets) {
		for (AlertTargetResponse target : targets) {
			if (notificationRepository.existsForEventAndAccount(event.eventId(), target.accountId())) {
				continue;
			}
			notificationRepository.save(new NotificationItem(
					idGenerator.newNotificationId(),
					target.accountId(),
					target.userId(),
					event.eventId(),
					event.sourceType(),
					event.title(),
					event.summary(),
					event.originalUrl(),
					event.stockCode(),
					event.matchingStockCodes(),
					target.matchReasons(),
					false,
					Instant.now(),
					null));
		}
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
				item.sourceType(),
				item.title(),
				item.summary(),
				item.originalUrl(),
				item.primaryStockCode(),
				item.matchedStockCodes(),
				item.matchReasons(),
				item.read(),
				item.createdAt(),
				item.readAt());
	}
}
