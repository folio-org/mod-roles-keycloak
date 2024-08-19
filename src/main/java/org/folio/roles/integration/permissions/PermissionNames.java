package org.folio.roles.integration.permissions;

import java.util.List;
import lombok.Data;

@Data
public class PermissionNames {

  private int totalRecords;
  private List<String> permissionNames;

  /**
   * Sets totalRecords for {@link PermissionNames} and returns {@link PermissionNames}.
   *
   * @return this {@link PermissionNames} with new totalRecords value
   */
  public PermissionNames totalRecords(int totalRecords) {
    this.totalRecords = totalRecords;
    return this;
  }

  /**
   * Sets permissionNames for {@link PermissionNames} and returns {@link PermissionNames}.
   *
   * @return this {@link PermissionNames} with new permissionNames value
   */
  public PermissionNames permissionNames(List<String> permissionNames) {
    this.permissionNames = permissionNames;
    return this;
  }
}
