package org.folio.roles.integration.permissions;

import java.util.List;
import lombok.Data;

@Data
public class Permissions {

  private int totalRecords;
  private List<String> permissionNames;

  /**
   * Sets totalRecords for {@link Permissions} and returns {@link Permissions}.
   *
   * @return this {@link Permissions} with new totalRecords value
   */
  public Permissions totalRecords(int totalRecords) {
    this.totalRecords = totalRecords;
    return this;
  }

  /**
   * Sets permissionNames for {@link Permissions} and returns {@link Permissions}.
   *
   * @return this {@link Permissions} with new permissionNames value
   */
  public Permissions permissionNames(List<String> permissionNames) {
    this.permissionNames = permissionNames;
    return this;
  }
}
