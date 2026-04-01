package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.repository.CapabilityRepository;
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
}
