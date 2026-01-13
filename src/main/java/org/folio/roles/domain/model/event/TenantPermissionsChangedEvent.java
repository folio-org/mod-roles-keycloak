package org.folio.roles.domain.model.event;

/**
 * Event published when tenant-wide permissions have changed.
 * This triggers cache eviction for all users in the current tenant.
 *
 * <p>This event is used for:
 * <ul>
 *   <li>Role capability changes (affects all users with that role)</li>
 *   <li>Capability registry changes (affects all users in tenant)</li>
 * </ul>
 */
public record TenantPermissionsChangedEvent() {

  /**
   * Factory method to create the event.
   *
   * @return new event instance
   */
  public static TenantPermissionsChangedEvent tenantPermissionsChanged() {
    return new TenantPermissionsChangedEvent();
  }
}
