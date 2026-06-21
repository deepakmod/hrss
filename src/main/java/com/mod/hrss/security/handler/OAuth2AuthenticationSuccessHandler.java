package com.mod.hrss.security.handler;

import com.mod.hrss.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.authorizedRedirectUri:http://localhost:3000/oauth2/redirect}")
    private String authorizedRedirectUri;

    @Value("${app.jwt.accessTokenExpirationInMs:900000}")
    private int accessTokenExpirationMs;

    @Value("${app.jwt.refreshTokenExpirationInMs:604800000}")
    private int refreshTokenExpirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", authorizedRedirectUri);
            return;
        }

        String accessToken  = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Set accessToken as HttpOnly cookie (short-lived, 15 min)
        addCookie(response, "accessToken", accessToken, accessTokenExpirationMs / 1000);

        // Set refreshToken as HttpOnly cookie (long-lived, 7 days)
        addCookie(response, "refreshToken", refreshToken, refreshTokenExpirationMs / 1000);

        log.info("OAuth2 login successful - tokens set as HttpOnly cookies");

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, authorizedRedirectUri);
    }

    /**
     * Creates a secure HttpOnly cookie by writing a raw Set-Cookie header.
     * We use the raw header (instead of response.addCookie) because the Jakarta
     * Cookie API does not expose the SameSite attribute, so we need full control.
     *
     * - HttpOnly  → JavaScript cannot access the cookie (XSS protection)
     * - Secure    → Only sent over HTTPS (set to true in production)
     * - SameSite=Lax → Allows top-level GET redirects (OAuth2 callback) while
     *                  blocking cross-site POST requests (CSRF protection)
     */
    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        String cookieHeader = name + "=" + value
                + "; Max-Age=" + maxAgeSeconds
                + "; Path=/"
                + "; HttpOnly"
                + "; SameSite=Lax";
        // Note: add "; Secure" here when deploying over HTTPS in production
        response.addHeader("Set-Cookie", cookieHeader);
    }
}


