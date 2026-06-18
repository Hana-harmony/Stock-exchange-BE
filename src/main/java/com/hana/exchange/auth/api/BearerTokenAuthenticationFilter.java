package com.hana.exchange.auth.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hana.exchange.account.application.AuthTokenService;
import com.hana.exchange.auth.application.ExchangeAuthenticationToken;
import com.hana.exchange.auth.domain.AuthenticatedExchangeUser;
import com.hana.exchange.common.api.ApiResponse;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final Pattern ACCOUNT_PATH_PATTERN = Pattern.compile("^/api/v1/accounts/([^/]+)(?:/.*)?$");
	private static final String BEARER_PREFIX = "Bearer ";
	private final AuthTokenService authTokenService;
	private final ObjectMapper objectMapper;

	public BearerTokenAuthenticationFilter(AuthTokenService authTokenService, ObjectMapper objectMapper) {
		this.authTokenService = authTokenService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !ACCOUNT_PATH_PATTERN.matcher(request.getRequestURI()).matches();
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		try {
			String accessToken = bearerToken(request);
			AuthTokenService.VerifiedToken token = authTokenService.verify(accessToken);
			assertAccountAccess(request, token.accountId());
			AuthenticatedExchangeUser principal = new AuthenticatedExchangeUser(
					token.userId(),
					token.username(),
					token.accountId(),
					token.issuedAt(),
					token.expiresAt());
			SecurityContextHolder.getContext()
					.setAuthentication(new ExchangeAuthenticationToken(principal, accessToken));
			filterChain.doFilter(request, response);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
			writeError(response, exception.errorCode());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private String bearerToken(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		String accessToken = authorization.substring(BEARER_PREFIX.length()).trim();
		if (accessToken.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		return accessToken;
	}

	private void assertAccountAccess(HttpServletRequest request, String tokenAccountId) {
		Matcher matcher = ACCOUNT_PATH_PATTERN.matcher(request.getRequestURI());
		if (matcher.matches() && !matcher.group(1).equals(tokenAccountId)) {
			throw new BusinessException(ErrorCode.AUTH_ACCOUNT_FORBIDDEN);
		}
	}

	private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.status().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ApiResponse
				.error(errorCode.status().value(), errorCode.code(), errorCode.message()));
	}
}
