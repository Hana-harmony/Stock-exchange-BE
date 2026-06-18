package com.hana.exchange.auth.application;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.hana.exchange.auth.domain.AuthenticatedExchangeUser;

public class ExchangeAuthenticationToken extends AbstractAuthenticationToken {

	private final AuthenticatedExchangeUser principal;
	private final String credentials;

	public ExchangeAuthenticationToken(AuthenticatedExchangeUser principal, String credentials) {
		super(List.of());
		this.principal = principal;
		this.credentials = credentials;
		setAuthenticated(true);
	}

	public ExchangeAuthenticationToken(
			AuthenticatedExchangeUser principal,
			String credentials,
			Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.principal = principal;
		this.credentials = credentials;
		setAuthenticated(true);
	}

	@Override
	public String getCredentials() {
		return credentials;
	}

	@Override
	public AuthenticatedExchangeUser getPrincipal() {
		return principal;
	}
}
