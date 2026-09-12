package com.sdt.web_app.config;

import com.sdt.web_app.service.authentication.TokenDenylistService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtDenylistValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenDenylistService denylistService;

    public JwtDenylistValidator(TokenDenylistService denylistService) {
        this.denylistService = denylistService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String jti = jwt.getId();
        String tokenValue = jwt.getTokenValue();

        if ((jti != null && denylistService.isRevoked(jti)) || denylistService.isRevoked(tokenValue)) {
            OAuth2Error error = new OAuth2Error("invalid_token", "The token has been revoked", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
