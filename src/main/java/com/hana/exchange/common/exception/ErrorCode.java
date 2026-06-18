package com.hana.exchange.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "Invalid request"),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON_002", "Request validation failed"),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_003", "Resource not found"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "Internal server error"),
	USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_001", "Username already exists"),
	INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "Invalid username or password"),
	INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "Invalid auth token"),
	AUTH_ACCOUNT_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "Authenticated account cannot access this account resource"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "Invalid refresh token"),
	MOCK_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_001", "Mock USD account not found"),
	WATCHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCHLIST_001", "Watchlist item not found"),
	ALERT_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ALERT_001", "Alert event not found"),
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_001", "Notification not found"),
	MARKET_UPSTREAM_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "MARKET_001", "Hana OmniLens market upstream is unavailable"),
	MOCK_ACCOUNT_INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "TRADE_001", "Mock USD account has insufficient balance"),
	MOCK_HOLDING_INSUFFICIENT_QUANTITY(HttpStatus.CONFLICT, "TRADE_002", "Mock holding has insufficient quantity"),
	MOCK_ORDER_BLOCKED(HttpStatus.CONFLICT, "TRADE_003", "Mock order is blocked by orderability rules"),
	TAX_CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "TAX_001", "Tax refund case not found");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public String message() {
		return message;
	}
}
