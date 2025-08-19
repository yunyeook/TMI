package com.tmi.backend.global.config;

import com.tmi.backend.domain.auth.jwt.filter.JwtAuthenticationFilter;
import com.tmi.backend.domain.auth.oauth.handler.OAuth2FailureHandler;
import com.tmi.backend.domain.auth.oauth.handler.OAuth2SuccessHandler;
import com.tmi.backend.domain.auth.oauth.service.CustomOAuth2UserService;
import com.tmi.backend.domain.auth.oauth.service.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * - JWT 기반 인증
 * - OAuth2 소셜 로그인 (카카오, 구글)
 * - Stateless 세션 정책
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // Form 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // 세션 비활성화 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스
                        .requestMatchers(SecurityPaths.STATIC_RESOURCES).permitAll()

                        // OAuth2 경로
                        .requestMatchers(SecurityPaths.OAUTH2_PATHS).permitAll()

                        // GET 요청 전체 허용 (개발 단계)
                        .requestMatchers(HttpMethod.GET, SecurityPaths.PUBLIC_GET_PATHS).permitAll()

                        // 특정 POST 요청 허용 (회원가입, 토큰 재발급, 로그아웃)
                        .requestMatchers(HttpMethod.POST, SecurityPaths.PUBLIC_POST_PATHS).permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated())

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/v1/oauth2/authorization"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))

                // JWT 필터 추가
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}