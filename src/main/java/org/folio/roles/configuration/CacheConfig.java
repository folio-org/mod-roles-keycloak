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

  @Value("${cache.permission-mappings.ttl}")
  private Duration permissionMappingsTtl;

  @Value("${cache.permission-mappings.max-size}")
  private int permissionMappingsMaxSize;

  @Value("${cache.keycloak-configuration.ttl}")
  private Duration keycloakConfigTtl;

  @Value("${cache.keycloak-configuration.max-size}")
  private int keycloakConfigMaxSize;

  @Value("${cache.keycloak-users.ttl}")
  private Duration keycloakUsersTtl;

  @Value("${cache.keycloak-users.max-size}")
  private int keycloakUsersMaxSize;

  @Value("${cache.keycloak-user-id.ttl}")
  private Duration keycloakUserIdTtl;

  @Value("${cache.keycloak-user-id.max-size}")
  private int keycloakUserIdMaxSize;

  @Value("${cache.authorization-client.ttl}")
  private Duration authorizationClientTtl;

  @Value("${cache.authorization-client.max-size}")
  private int authorizationClientMaxSize;

  @Value("${cache.user-permissions.ttl}")
  private Duration userPermissionsTtl;

  @Value("${cache.user-permissions.max-size}")
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

  private CaffeineCache buildCache(String name, int maxSize, Duration ttl) {
    return new CaffeineCache(
      name,
      Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttl)
        .build()
    );
  }
}
