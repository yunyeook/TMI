package com.tmi.backend.domain.auth.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmi.backend.domain.auth.jwt.provider.JwtTokenProvider;
import com.tmi.backend.global.common.response.ApiResponse;
import com.tmi.backend.global.common.response.impl.ApiErrorResponse;
import com.tmi.backend.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 토큰을 검증하고 SecurityContext에 인증 정보를 설정하는 필터
 * SecurityConfig의 permitAll() 설정에 따라 접근 제어가 이루어지므로,
 * 이 필터는 JWT 검증에만 집중합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;

  private static final String ACCESS_TOKEN_COOKIE_NAME = "ACCESS_TOKEN";

  /**
   * CORS Preflight 요청은 JWT 검증을 건너뜁니다.
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      String token = resolveAccessToken(request);

      // 토큰이 존재하는 경우에만 검증
      if (token != null) {
        validateAndSetAuthentication(token, response);
      }
      // 토큰이 없으면 그냥 통과 (SecurityConfig의 permitAll이 처리)

      filterChain.doFilter(request, response);

    } catch (Exception e) {
      log.error("JWT 필터 처리 중 예외 발생", e);
      sendErrorResponse(response, ErrorCode.AUTH_INVALID_TOKEN);
    }
  }

  /**
   * JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정
   */
  private void validateAndSetAuthentication(String token, HttpServletResponse response)
      throws IOException {
    if (jwtTokenProvider.validateToken(token)) {
      Authentication authentication = jwtTokenProvider.getAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.debug("JWT 인증 성공 - Principal: {}", authentication.getName());
    } else {
      log.warn("유효하지 않은 JWT 토큰");
      sendErrorResponse(response, ErrorCode.AUTH_INVALID_TOKEN);
    }
  }

  /**
   * 쿠키에서 ACCESS_TOKEN을 추출
   */
  private String resolveAccessToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    return Arrays.stream(cookies)
        .filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
        .findFirst()
        .map(Cookie::getValue)
        .orElse(null);
  }

  /**
   * 에러 응답을 JSON 형식으로 반환
   */
  private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    ApiResponse<Void> errorResponse = ApiErrorResponse.error(errorCode);

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), errorResponse);
  }
}