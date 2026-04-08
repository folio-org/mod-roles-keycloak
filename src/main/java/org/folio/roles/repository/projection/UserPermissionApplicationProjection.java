package org.folio.roles.repository.projection;

/**
 * Projection for user permission to application mapping query.
 */
public interface UserPermissionApplicationProjection {

  /**
   * Returns the FOLIO permission name (from capability.folio_permission).
   */
  String getPermission();

  /**
   * Returns the application ID that owns this capability.
   */
  String getApplicationId();

  /**
   * Returns the array of replaced permission names (from the permission.replaces column), may be null.
   */
  String[] getReplaces();
}
