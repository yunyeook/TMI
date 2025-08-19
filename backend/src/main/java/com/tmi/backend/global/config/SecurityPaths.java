package com.tmi.backend.global.config;

/**
 * Spring Security 경로 설정을 중앙에서 관리하는 상수 클래스
 */
public final class SecurityPaths {

    private SecurityPaths() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 정적 리소스 경로
     */
    public static final String[] STATIC_RESOURCES = {
            "/",
            "/favicon.ico",
            "/error",
            "/css/**",
            "/js/**",
            "/images/**",
            "/assets/**",
            "/webjars/**",
            "/.well-known/**"
    };

    /**
     * OAuth2 관련 경로
     */
    public static final String[] OAUTH2_PATHS = {
            "/oauth2/**",
            "/api/v1/oauth2/**",
            "/login/oauth2/code/**",
            "/login"
    };

    /**
     * 인증 없이 접근 가능한 POST 요청 경로
     */
    public static final String[] PUBLIC_POST_PATHS = {
            "/api/v1/member/signup",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout/**"
    };

    /**
     * 인증 없이 접근 가능한 GET 요청 경로 (모든 조회 API)
     */
    public static final String[] PUBLIC_GET_PATHS = {
            "/api/v1/**"
    };
}