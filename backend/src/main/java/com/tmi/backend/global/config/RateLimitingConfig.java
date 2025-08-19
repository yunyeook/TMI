package com.tmi.backend.global.config;

import com.tmi.backend.global.ratelimit.RateLimitingFilter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RateLimitingConfig {

  private final RateLimitingFilter rateLimitingFilter;

  @Value("${spring.data.redis.host}")
  private String redisHost;

  @Value("${spring.data.redis.port}")
  private int redisPort;

  @Bean
  public RedisClient redisClient() {
    return RedisClient.create(RedisURI.builder()
        .withHost(redisHost)
        .withPort(redisPort)
        .build());
  }

  @Bean
  public ProxyManager<byte[]> lettuceBasedProxyManager(RedisClient redisClient) {
    return LettuceBasedProxyManager.builderFor(redisClient)
        .withClientSideConfig(
            ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                    // 마지막으로 요청을 보낸 시점으로부터 2일 뒤에 레디스에서 기록이 삭제됨.
                    ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        Duration.ofDays(2))))
        .build();
  }

  @Bean
  public FilterRegistrationBean<RateLimitingFilter> rateLimiterFilter() {
    FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(rateLimitingFilter); // 기본적으로 필터의 가장 마지막에 추가됨.
    registrationBean.addUrlPatterns("/api/v1/summary"); // 제한 적용할 URL
    return registrationBean;
  }
}
