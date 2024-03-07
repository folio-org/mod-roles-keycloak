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
public class LoadablePermissionKey implements Serializable {

  @Serial private static final long serialVersionUID = 5181693955891076504L;

  private UUID roleId;

  private String permissionName;
}
