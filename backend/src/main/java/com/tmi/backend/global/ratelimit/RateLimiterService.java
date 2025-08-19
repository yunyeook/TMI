package com.tmi.backend.global.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterService {

  private final ProxyManager<byte[]> proxyManager;

  public Bucket resolveBucket(String userId) {
    Supplier<BucketConfiguration> configSupplier = this::getConfig;

    // Redis에서 버킷을 가져오거나 없으면 생성
    byte[] key = userId.getBytes(StandardCharsets.UTF_8);
    return proxyManager.builder().build(key, configSupplier);
  }

  private BucketConfiguration getConfig() {
    return BucketConfiguration.builder()
        .addLimit(Bandwidth.builder()
            .capacity(3) // 최대 3회
            .refillIntervally(3, Duration.ofDays(1)) // 하루 단위로 3개 리필
            .build())
        .build();
  }
}
