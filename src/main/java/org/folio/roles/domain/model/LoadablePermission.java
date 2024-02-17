package org.folio.roles.domain.model;

import static org.apache.commons.lang3.StringUtils.isBlank;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Metadata;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class LoadablePermission {

  @Valid
  private UUID roleId;
  private String permissionName;
  @Valid
  private UUID capabilityId;
  @Valid
  private UUID capabilitySetId;
  @Valid
  private Metadata metadata;

  public static LoadablePermission of(UUID roleId, String permissionName) {
    if (isBlank(permissionName)) {
      throw new IllegalArgumentException("Permission name is blank");
    }

    var result = new LoadablePermission();
    result.setRoleId(roleId);
    result.setPermissionName(permissionName);

    return result;
  }

  public LoadablePermission roleId(UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  public LoadablePermission permissionName(String permissionName) {
    this.permissionName = permissionName;
    return this;
  }

  public LoadablePermission capabilityId(UUID capabilityId) {
    this.capabilityId = capabilityId;
    return this;
  }

  public LoadablePermission capabilitySetId(UUID capabilitySetId) {
    this.capabilitySetId = capabilitySetId;
    return this;
  }

  public LoadablePermission metadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }
}
