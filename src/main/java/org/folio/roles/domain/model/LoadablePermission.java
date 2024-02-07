package org.folio.roles.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Metadata;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode()
public class LoadablePermission {

  @NotNull
  private String permissionName;

  @Valid
  private UUID capabilityId;

  @Valid
  private UUID capabilitySetId;

  @Valid
  private Metadata metadata;

  public static LoadablePermission of(String permissionName) {
    var result = new LoadablePermission();
    result.setPermissionName(permissionName);

    return result;
  }
}
