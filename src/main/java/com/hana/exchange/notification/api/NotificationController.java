package com.hana.exchange.notification.api;

import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.notification.application.NotificationService;
import com.hana.exchange.notification.domain.NotificationInboxResponse;
import com.hana.exchange.notification.domain.NotificationItemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Local in-app notification inbox APIs")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	@Operation(summary = "Get in-app notifications for an account")
	public ApiResponse<NotificationInboxResponse> getNotifications(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(notificationService.getInbox(accountId));
	}

	@PostMapping("/{notificationId}/read")
	@Operation(summary = "Mark an in-app notification as read")
	public ApiResponse<NotificationItemResponse> markRead(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@PathVariable @Pattern(regexp = "NTF-[A-Z0-9]{12}") String notificationId) {
		return ApiResponse.success(notificationService.markRead(accountId, notificationId));
	}
}
