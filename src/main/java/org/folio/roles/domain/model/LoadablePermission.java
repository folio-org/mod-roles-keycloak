package org.folio.roles.domain.model;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
  private UUID roleId;

  @NotNull
  private String permissionName;

  @Valid
  private UUID capabilityId;

  @Valid
  private UUID capabilitySetId;

  @Valid
  private Metadata metadata;

  public static LoadablePermission of(UUID roleId, String permissionName) {
    if (roleId == null) {
      throw new IllegalArgumentException("Role id is null");
    }
    if (isBlank(permissionName)) {
      throw new IllegalArgumentException("Permission name is blank");
    }

    var result = new LoadablePermission();
    result.setRoleId(roleId);
    result.setPermissionName(permissionName);

    return result;
  }
}
