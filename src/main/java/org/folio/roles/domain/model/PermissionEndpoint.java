package org.folio.roles.domain.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PermissionEndpoint {

  /**
   * Permission identifier.
   */
  private UUID permissionId;

  /**
   * Endpoint identifier.
   */
  private UUID endpointId;
}
