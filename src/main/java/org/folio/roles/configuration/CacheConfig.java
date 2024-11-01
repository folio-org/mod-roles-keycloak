package org.folio.roles.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  @Value("${cache.permission-mappings.ttl:60}")
  private int cacheTtl;

  @Bean
  public CacheManager cacheManager() {
    var cacheManager = new CaffeineCacheManager("permission-mappings");
    cacheManager.setCaffeine(
      Caffeine.newBuilder().initialCapacity(1).expireAfterWrite(Duration.ofSeconds(cacheTtl)).maximumSize(1));
    return cacheManager;
  }
}
