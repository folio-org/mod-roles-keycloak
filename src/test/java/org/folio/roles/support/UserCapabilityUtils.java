package org.folio.roles.support;

import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.TestConstants.USER_ID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.UserCapabilities;
import org.folio.roles.domain.dto.UserCapabilitiesRequest;
import org.folio.roles.domain.dto.UserCapability;
import org.folio.roles.domain.entity.UserCapabilityEntity;

@UtilityClass
public class UserCapabilityUtils {

  public static UserCapabilityEntity userCapabilityEntity() {
    return userCapabilityEntity(USER_ID, CAPABILITY_ID);
  }

  public static UserCapabilityEntity userCapabilityEntity(UUID capabilityId) {
    return userCapabilityEntity(USER_ID, capabilityId);
  }

  public static UserCapabilityEntity userCapabilityEntity(UUID userId, UUID capabilityId) {
    var result = new UserCapabilityEntity();

    result.setUserId(userId);
    result.setCapabilityId(capabilityId);

    return result;
  }

  public static UserCapability userCapability() {
    return userCapability(USER_ID, CAPABILITY_ID);
  }

  public static UserCapability userCapability(UUID capabilityId) {
    return userCapability(USER_ID, capabilityId);
  }

  public static UserCapability userCapability(UUID userId, UUID capabilityId) {
    return new UserCapability().userId(userId).capabilityId(capabilityId);
  }

  public static UserCapabilities userCapabilities(UserCapability... values) {
    return userCapabilities(values.length, values);
  }

  public static UserCapabilities userCapabilities(long totalRecords, UserCapability... values) {
    return new UserCapabilities().userCapabilities(List.of(values)).totalRecords(totalRecords);
  }

  public static UserCapabilitiesRequest userCapabilitiesRequest(UUID userId, UUID... capabilityIds) {
    return new UserCapabilitiesRequest().userId(userId).capabilityIds(Arrays.asList(capabilityIds));
  }

  public static UserCapabilitiesRequest userCapabilitiesRequest(UUID userId, List<UUID> capabilityIds) {
    return new UserCapabilitiesRequest().userId(userId).capabilityIds(capabilityIds);
  }
}
