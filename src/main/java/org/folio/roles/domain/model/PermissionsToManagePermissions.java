package org.folio.roles.domain.model;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PermissionsToManagePermissions {

  /**
   * Permissions to view permissions.
   */
  private Set<String> viewPermissions = new HashSet<>();

  /**
   * Permissions to edit permissions.
   */
  private Set<String> editPermissions = new HashSet<>();
}
