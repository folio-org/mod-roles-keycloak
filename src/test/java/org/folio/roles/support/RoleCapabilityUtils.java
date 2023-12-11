package org.folio.roles.support;

import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.RoleUtils.ROLE_ID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.RoleCapabilities;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapability;
import org.folio.roles.domain.entity.RoleCapabilityEntity;

@UtilityClass
public class RoleCapabilityUtils {

  public static RoleCapabilityEntity roleCapabilityEntity() {
    return roleCapabilityEntity(ROLE_ID, CAPABILITY_ID);
  }

  public static RoleCapabilityEntity roleCapabilityEntity(UUID capabilityId) {
    return roleCapabilityEntity(ROLE_ID, capabilityId);
  }

  public static RoleCapabilityEntity roleCapabilityEntity(UUID roleId, UUID capabilityId) {
    var result = new RoleCapabilityEntity();

    result.setRoleId(roleId);
    result.setCapabilityId(capabilityId);

    return result;
  }

  public static RoleCapability roleCapability() {
    return roleCapability(ROLE_ID, CAPABILITY_ID);
  }

  public static RoleCapability roleCapability(UUID capabilityId) {
    return roleCapability(ROLE_ID, capabilityId);
  }

  public static RoleCapability roleCapability(UUID roleId, UUID capabilityId) {
    return new RoleCapability().roleId(roleId).capabilityId(capabilityId);
  }

  public static RoleCapabilities roleCapabilities(RoleCapability... values) {
    return roleCapabilities(values.length, values);
  }

  public static RoleCapabilities roleCapabilities(long totalRecords, RoleCapability... values) {
    return new RoleCapabilities().roleCapabilities(List.of(values)).totalRecords(totalRecords);
  }

  public static RoleCapabilitiesRequest roleCapabilitiesRequest(UUID roleId, UUID... capabilityIds) {
    return new RoleCapabilitiesRequest().roleId(roleId).capabilityIds(Arrays.asList(capabilityIds));
  }

  public static RoleCapabilitiesRequest roleCapabilitiesRequest(UUID roleId, List<UUID> capabilityIds) {
    return new RoleCapabilitiesRequest().roleId(roleId).capabilityIds(capabilityIds);
  }
}
