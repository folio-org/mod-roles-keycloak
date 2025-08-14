package org.folio.roles.service.capability;

import static java.util.Objects.requireNonNull;
import static org.folio.common.utils.permission.PermissionUtils.hasNoRequiredFields;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;

import lombok.RequiredArgsConstructor;
import org.folio.common.utils.permission.PermissionUtils;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.service.permission.PermissionOverrider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CapabilityResolver {

  private final PermissionOverrider permissionOverrider;

  public boolean isCapabilityPermissionCorrupted(Capability capability) {
    requireNonNull(capability.getPermission(), "Permission cannot be null");

    var permission = capability.getPermission();
    var permissionData = extractPermissionData(permission);
    if (hasNoRequiredFields(permissionData)) {
      throw new IllegalArgumentException(); //todo what is right behaviour here?
    }

    var expectedName = getCapabilityName(permissionData);
    return !expectedName.equals(capability.getName());
  }

  private PermissionData extractPermissionData(String permissionName) {
    var mappings = permissionOverrider.getPermissionMappings();
    var permissionData = mappings.get(permissionName);
    return permissionData != null ? permissionData : PermissionUtils.extractPermissionData(permissionName);
  }
}
