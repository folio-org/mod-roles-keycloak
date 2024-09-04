package org.folio.roles.utils;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.common.utils.permission.model.PermissionAction;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.model.PageResult;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CapabilityUtils {

  /**
   * Generates capability name from permission resource and action.
   *
   * @param resource - permission resource name as {@link String}
   * @param action - permission action as {@link PermissionAction}
   * @return generated capability name
   */
  public static String getCapabilityName(String resource, PermissionAction action) {
    var newResourceName = resource.toLowerCase().replaceAll("\\s+", "_");
    return newResourceName + "." + action.getValue();
  }

  /**
   * Generates capability name from resource and action.
   *
   * @param resource - capability resource name as {@link String}
   * @param action - capability action as {@link CapabilityAction}
   * @return generated capability name
   */
  public static String getCapabilityName(String resource, CapabilityAction action) {
    var newResourceName = resource.toLowerCase().replaceAll("\\s+", "_");
    return newResourceName + "." + action.getValue();
  }

  /**
   * Generates capability name from {@link PermissionData} object.
   *
   * @param permissionData - permission data as {@link PermissionData} object
   * @return generated capability name
   */
  public static String getCapabilityName(PermissionData permissionData) {
    return getCapabilityName(permissionData.getResource(), permissionData.getAction());
  }

  /**
   * Retrieves capability ids from {@link PageResult} with {@link CapabilitySet} values.
   *
   * @param capabilitySets - {@link PageResult} with {@link CapabilitySet} values
   * @return {@link List} with {@link UUID} values
   */
  public static List<UUID> getCapabilitySetIds(PageResult<CapabilitySet> capabilitySets) {
    return toStream(capabilitySets.getRecords())
      .map(CapabilitySet::getCapabilities)
      .filter(CollectionUtils::isNotEmpty)
      .flatMap(Collection::stream)
      .distinct()
      .toList();
  }

  /**
   * Returns {@link List} with unique {@link Endpoint} object from {@link Collection} with {@link Capability} values.
   *
   * @param capabilities - {@link Collection} with {@link Capability} values
   * @return {@link List} with unique {@link Endpoint} values
   */
  public static List<Endpoint> getCapabilityEndpoints(Collection<Capability> capabilities) {
    return toStream(capabilities)
      .map(Capability::getEndpoints)
      .filter(CollectionUtils::isNotEmpty)
      .flatMap(Collection::stream)
      .distinct()
      .toList();
  }

  public static boolean isTechnicalCapability(Capability capability) {
    return isEmpty(capability.getEndpoints());
  }

  public static void verifyRequest(RoleCapabilitiesRequest request) {
    verifyRequest(request.getCapabilityIds(), request.getCapabilityNames());
  }

  public static void verifyRequest(CapabilitiesUpdateRequest request) {
    verifyRequest(request.getCapabilityIds(), request.getCapabilityNames());
  }

  public static void verifyRequest(RoleCapabilitySetsRequest request) {
    verifyRequest(request.getCapabilitySetIds(), request.getCapabilitySetNames());
  }

  public static void verifyRequest(CapabilitySetsUpdateRequest request) {
    verifyRequest(request.getCapabilitySetIds(), request.getCapabilitySetNames());
  }

  private static void verifyRequest(List<UUID> capabilityIds, List<String> capabilityNames ) {
    if (isEmpty(capabilityIds) && isEmpty(capabilityNames)) {
      throw new IllegalArgumentException("'capabilityIds' or 'capabilityNames' must not be null");
    }
  }
}
