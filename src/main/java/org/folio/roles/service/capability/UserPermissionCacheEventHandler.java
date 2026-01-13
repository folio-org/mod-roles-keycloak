package org.folio.roles.service.capability;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.model.event.TenantPermissionsChangedEvent;
import org.folio.roles.domain.model.event.UserPermissionsChangedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles domain events that require user permission cache eviction.
 *
 * <p>All handlers use AFTER_COMMIT phase to ensure cache eviction occurs only after
 * successful database commits. Handlers are deliberately simple with no transaction management or exception handling,
 * as the underlying cache eviction methods are already designed as best-effort operations with internal error
 * handling.
 *
 * <p>FolioExecutionContext is accessed via injection (backed by ThreadLocal) and is
 * automatically available because {@code @TransactionalEventListener} executes synchronously on the same thread as the
 * event publisher.
 */
@Service
@RequiredArgsConstructor
public class UserPermissionCacheEventHandler {

  private final UserPermissionsCacheEvictor userPermissionsCacheEvictor;

  /**
   * Handles user-specific permission changes (direct capability/capability-set/role assignments). Evicts cache for the
   * affected user only.
   *
   * @param event - contains userId
   */
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleUserPermissionsChanged(UserPermissionsChangedEvent event) {
    userPermissionsCacheEvictor.evictUserPermissions(event.userId());
  }

  /**
   * Handles tenant-wide permission changes (role capability changes, capability registry changes). Evicts cache for
   * entire tenant.
   *
   * @param event - empty event (tenant context accessed from FolioExecutionContext)
   */
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleTenantPermissionsChanged(TenantPermissionsChangedEvent event) {
    userPermissionsCacheEvictor.evictUserPermissionsForCurrentTenant();
  }
}
