package com.tmi.backend.domain.auth.oauth.handler;

import com.tmi.backend.domain.auth.jwt.service.TokenService;
import com.tmi.backend.domain.auth.oauth.util.CustomOauthUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 로그인 성공 시 처리 핸들러
 * - 신규 회원: 회원가입 페이지로 리다이렉트 (임시 토큰 발급)
 * - 기존 회원: 메인 페이지로 리다이렉트 (JWT 토큰 발급)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final TokenService tokenService;

  @Value("${frontend.redirect-uri}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(
          HttpServletRequest request,
          HttpServletResponse response,
          Authentication authentication
  ) throws IOException {

    CustomOauthUser customUser = (CustomOauthUser) authentication.getPrincipal();

    log.info("OAuth2 로그인 성공 - Provider: {}, MemberId: {}, IsNew: {}",
            customUser.getProvider(),
            customUser.getMemberId(),
            customUser.isNewUser());

    // 세션 정리 (OAuth2 로그인 과정에서 생성된 세션 제거)
    clearSession(request, response);

    // 신규/기존 회원 분기 처리
    String targetUrl = customUser.isNewUser()
            ? handleNewMember(response, customUser)
            : handleExistingMember(response, customUser);

    response.sendRedirect(targetUrl);
  }

  /**
   * 세션 및 JSESSIONID 쿠키 정리
   */
  private void clearSession(HttpServletRequest request, HttpServletResponse response) {
    // 세션 무효화
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    // JSESSIONID 쿠키 삭제
    Cookie jsessionCookie = new Cookie("JSESSIONID", null);
    jsessionCookie.setMaxAge(0);
    jsessionCookie.setPath("/");
    jsessionCookie.setHttpOnly(true);
    response.addCookie(jsessionCookie);
  }

  /**
   * 신규 회원 처리
   * - 임시 등록 토큰 발급 (5분 유효)
   * - 회원가입 페이지로 리다이렉트
   */
  private String handleNewMember(HttpServletResponse response, CustomOauthUser user) {
    tokenService.createAndAddRegistCookie(
            response,
            user.getProvider().name(),
            user.getProviderMemberId()
    );

    return UriComponentsBuilder.fromHttpUrl(redirectUri)
            .queryParam("isNew", true)
            .queryParam("provider", user.getProvider().name())
            .queryParam("providerMemberId", user.getProviderMemberId())
            .toUriString();
  }

  /**
   * 기존 회원 처리
   * - JWT 액세스/리프레시 토큰 발급
   * - 메인 페이지로 리다이렉트
   */
  private String handleExistingMember(HttpServletResponse response, CustomOauthUser user) {
    tokenService.createAndAddAuthCookies(response, user.getMemberId());

    return UriComponentsBuilder.fromHttpUrl(redirectUri)
            .queryParam("isNew", false)
            .queryParam("memberId", user.getMemberId())
            .toUriString();
  }
}