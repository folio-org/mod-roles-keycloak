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
public class UserCapabilityKey implements Serializable {

  @Serial private static final long serialVersionUID = -7595449844304869014L;

  /**
   * Role identifier.
   */
  private UUID userId;

  /**
   * Capability identifier.
   */
  private UUID capabilityId;
}
