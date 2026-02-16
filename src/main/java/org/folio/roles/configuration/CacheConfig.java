package org.folio.roles.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  @Value("${cache.permission-mappings.ttl:60}")
  private int permissionMappingsTtl;

  @Value("${cache.permission-mappings.max-size:1}")
  private int permissionMappingsMaxSize;

  @Value("${cache.keycloak-configuration.ttl:3600}")
  private int keycloakConfigTtl;

  @Value("${cache.keycloak-configuration.max-size:100}")
  private int keycloakConfigMaxSize;

  @Value("${cache.keycloak-users.ttl:180}")
  private int keycloakUsersTtl;

  @Value("${cache.keycloak-users.max-size:250}")
  private int keycloakUsersMaxSize;

  @Value("${cache.keycloak-user-id.ttl:180}")
  private int keycloakUserIdTtl;

  @Value("${cache.keycloak-user-id.max-size:250}")
  private int keycloakUserIdMaxSize;

  @Value("${cache.authorization-client.ttl:3600}")
  private int authorizationClientTtl;

  @Value("${cache.authorization-client.max-size:100}")
  private int authorizationClientMaxSize;

  @Value("${cache.user-permissions.ttl:30}")
  private int userPermissionsTtl;

  @Value("${cache.user-permissions.max-size:1000}")
  private int userPermissionsMaxSize;

  @Bean
  public CacheManager cacheManager() {
    var cacheManager = new SimpleCacheManager();

    var caches = Arrays.asList(
      buildCache("permission-mappings", permissionMappingsMaxSize, permissionMappingsTtl),
      buildCache("keycloak-configuration", keycloakConfigMaxSize, keycloakConfigTtl),
      buildCache("keycloak-users", keycloakUsersMaxSize, keycloakUsersTtl),
      buildCache("keycloak-user-id", keycloakUserIdMaxSize, keycloakUserIdTtl),
      buildCache("authorization-client-cache", authorizationClientMaxSize, authorizationClientTtl),
      buildCache("user-permissions", userPermissionsMaxSize, userPermissionsTtl)
    );

    cacheManager.setCaches(caches);
    return cacheManager;
  }

  private CaffeineCache buildCache(String name, int maxSize, int ttlSeconds) {
    return new CaffeineCache(
      name,
      Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
        .build()
    );
  }
}
