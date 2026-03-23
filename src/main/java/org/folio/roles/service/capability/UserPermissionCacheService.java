package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.service.capability.model.UserPermissionMappings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for caching user permissions.
 *
 * <p>Separated from CapabilityService to ensure Spring AOP proxy intercepts @Cacheable calls.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UserPermissionCacheService {

  private final CapabilityRepository capabilityRepository;

  /**
   * Retrieves all user permissions from database (cached).
   *
   * @param userId - user identifier
   * @return list of folio permission names for the user, or empty list if user has no permissions
   */
  @Cacheable(cacheNames = "user-permissions", key = "@folioExecutionContext.tenantId + ':' + #userId")
  @Transactional(readOnly = true)
  public List<String> getAllUserPermissions(UUID userId) {
    log.debug("Cache miss: loading permissions for user: {}", userId);
    var permissions = capabilityRepository.findAllFolioPermissions(userId);
    return permissions != null ? permissions : emptyList();
  }

  /**
   * Retrieves user permission-to-application mappings from database (cached).
   *
   * <p>The returned {@link UserPermissionMappings} contains all permission names (including replaced ones)
   * and a map from each permission name to the application ID that owns it.
   *
   * @param userId - user identifier
   * @return {@link UserPermissionMappings} with permission list and application ID mapping
   */
  @Cacheable(cacheNames = "user-permissions", key = "@folioExecutionContext.tenantId + ':' + #userId + ':mappings'")
  @Transactional(readOnly = true)
  public UserPermissionMappings getUserPermissionMappings(UUID userId) {
    log.debug("Cache miss: loading permission mappings for user: {}", userId);
    var rows = capabilityRepository.findAllUserPermissionMappings(userId);
    var permissionToApplicationId = new LinkedHashMap<String, String>();
    for (var row : rows) {
      permissionToApplicationId.putIfAbsent(row.getPermission(), row.getApplicationId());
      for (var replaced : nullToEmpty(row.getReplaces())) {
        permissionToApplicationId.putIfAbsent(replaced, row.getApplicationId());
      }
    }
    return new UserPermissionMappings(new ArrayList<>(permissionToApplicationId.keySet()),
      Map.copyOf(permissionToApplicationId));
  }
}
