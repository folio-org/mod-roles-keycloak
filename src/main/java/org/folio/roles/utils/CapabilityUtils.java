package org.folio.roles.utils;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.model.PageResult;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CapabilityUtils {

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
   * Generates capability name from raw {@link Capability} object.
   *
   * @param capability - capability as {@link Capability} object
   * @return generated capability name
   */
  public static String getCapabilityName(Capability capability) {
    var resource = capability.getResource();
    var action = capability.getAction();
    var newResourceName = resource.toLowerCase().replaceAll("\\s+", "_");
    return newResourceName + "." + action.getValue();
  }

  /**
   * Retrieves capability ids from {@link PageResult} with {@link CapabilitySet} values.
   *
   * @param capabilitySets - {@link PageResult} with {@link CapabilitySet} values
   * @return {@link List} with {@link UUID} values
   */
  public static List<UUID> getCapabilityIds(PageResult<CapabilitySet> capabilitySets) {
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
}
