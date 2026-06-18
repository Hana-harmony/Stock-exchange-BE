package com.hana.exchange.account.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.AuthProperties;

@Service
public class AuthTokenService {

	private static final String ALGORITHM = "HmacSHA256";
	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
	private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
	private static final Pattern LONG_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");
	private final AuthProperties authProperties;

	public AuthTokenService(AuthProperties authProperties) {
		this.authProperties = authProperties;
	}

	public IssuedToken issue(ExchangeUser user, MockUsdAccount account) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(authProperties.accessTokenTtl());
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", user.userId());
		claims.put("username", user.username());
		claims.put("accountId", account.accountId());
		claims.put("iat", issuedAt.getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());
		String header = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
		String payload = base64Url(toJson(claims).getBytes(StandardCharsets.UTF_8));
		String signature = signature(header + "." + payload);
		return new IssuedToken("Bearer", header + "." + payload + "." + signature, issuedAt, expiresAt);
	}

	public VerifiedToken verify(String accessToken) {
		String[] parts = accessToken.split("\\.");
		if (parts.length != 3) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		String expectedSignature = signature(parts[0] + "." + parts[1]);
		if (!MessageDigest.isEqual(
				expectedSignature.getBytes(StandardCharsets.UTF_8),
				parts[2].getBytes(StandardCharsets.UTF_8))) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		try {
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			String userId = stringField(payload, "sub");
			String username = stringField(payload, "username");
			String accountId = stringField(payload, "accountId");
			Instant issuedAt = Instant.ofEpochSecond(longField(payload, "iat"));
			Instant expiresAt = Instant.ofEpochSecond(longField(payload, "exp"));
			if (!expiresAt.isAfter(Instant.now())) {
				throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
			}
			return new VerifiedToken(userId, username, accountId, issuedAt, expiresAt);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
	}

	private String signature(String signingInput) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(authProperties.tokenSigningKey().getBytes(StandardCharsets.UTF_8), ALGORITHM));
			return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Auth token signing failed", exception);
		}
	}

	private String base64Url(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String toJson(Map<String, Object> claims) {
		StringBuilder builder = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<String, Object> entry : claims.entrySet()) {
			if (!first) {
				builder.append(",");
			}
			builder.append("\"").append(entry.getKey()).append("\":");
			if (entry.getValue() instanceof Number) {
				builder.append(entry.getValue());
			} else {
				builder.append("\"").append(entry.getValue()).append("\"");
			}
			first = false;
		}
		return builder.append("}").toString();
	}

	private String stringField(String payload, String field) {
		Matcher matcher = Pattern.compile(STRING_FIELD_PATTERN.pattern().formatted(field)).matcher(payload);
		if (!matcher.find()) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		return matcher.group(1);
	}

	private long longField(String payload, String field) {
		Matcher matcher = Pattern.compile(LONG_FIELD_PATTERN.pattern().formatted(field)).matcher(payload);
		if (!matcher.find()) {
			throw new BusinessException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		return Long.parseLong(matcher.group(1));
	}

	public record IssuedToken(
			String tokenType,
			String accessToken,
			Instant issuedAt,
			Instant expiresAt
	) {
	}

	public record VerifiedToken(
			String userId,
			String username,
			String accountId,
			Instant issuedAt,
			Instant expiresAt
	) {
	}
}
