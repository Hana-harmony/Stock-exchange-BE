package com.hana.exchange.account.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hana.exchange.account.application.AccountService;
import com.hana.exchange.account.domain.AccountBalanceResponse;
import com.hana.exchange.account.domain.DepositUsdRequest;
import com.hana.exchange.account.domain.LoginRequest;
import com.hana.exchange.account.domain.LoginResponse;
import com.hana.exchange.account.domain.SignUpRequest;
import com.hana.exchange.account.domain.SignUpResponse;
import com.hana.exchange.account.domain.TokenVerifyRequest;
import com.hana.exchange.account.domain.TokenVerifyResponse;
import com.hana.exchange.common.api.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Account", description = "Local exchange mock user and USD account APIs")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping("/auth/signup")
	@Operation(summary = "Sign up a local exchange user and create a mock USD account")
	public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ApiResponse.success(accountService.signUp(request));
	}

	@PostMapping("/auth/login")
	@Operation(summary = "Login a local exchange user and issue a local JWT")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(accountService.login(request));
	}

	@PostMapping("/auth/token/verify")
	@Operation(summary = "Verify a local exchange JWT issued by the login API")
	public ApiResponse<TokenVerifyResponse> verifyToken(@Valid @RequestBody TokenVerifyRequest request) {
		return ApiResponse.success(accountService.verifyToken(request));
	}

	@GetMapping("/accounts/{accountId}")
	@Operation(summary = "Get a mock USD account balance", security = @SecurityRequirement(name = "bearerAuth"))
	public ApiResponse<AccountBalanceResponse> getAccount(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId) {
		return ApiResponse.success(accountService.getAccount(accountId));
	}

	@PostMapping("/accounts/{accountId}/deposits")
	@Operation(
			summary = "Deposit mock USD without real payment settlement",
			security = @SecurityRequirement(name = "bearerAuth"))
	public ApiResponse<AccountBalanceResponse> depositUsd(
			@PathVariable @Pattern(regexp = "ACC-[A-Z0-9]{12}") String accountId,
			@Valid @RequestBody DepositUsdRequest request) {
		return ApiResponse.success(accountService.depositUsd(accountId, request));
	}
}
