package org.folio.roles.domain.entity.key;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class UserRoleKey implements Serializable {

  @Serial private static final long serialVersionUID = 6392439547837329203L;

  /**
   * User identifier.
   */
  private UUID userId;

  /**
   * Role identifier.
   */
  private UUID roleId;
}
