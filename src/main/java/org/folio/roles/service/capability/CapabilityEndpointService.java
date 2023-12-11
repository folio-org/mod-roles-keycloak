package org.folio.roles.service.capability;

import static org.folio.roles.utils.CapabilityUtils.getCapabilityEndpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.folio.roles.domain.dto.Endpoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CapabilityEndpointService {

  private final CapabilityService capabilityService;

  /**
   * Provides a list of changed endpoints using changed and assigned capability identifiers.
   *
   * @param changedIds - list with changed capability identifiers
   * @param assignedIds - list with assigned capability identifiers
   * @return list with changed {@link Endpoint} objects
   */
  @Transactional(readOnly = true)
  public List<Endpoint> getByCapabilityIds(Collection<UUID> changedIds, Collection<UUID> assignedIds) {
    var changedIdentifiers = CollectionUtils.subtract(changedIds, assignedIds);
    var changedCapabilityEndpoints = getCapabilityEndpoints(capabilityService.findByIds(changedIdentifiers));
    var assignedCapabilityEndpoints = getCapabilityEndpoints(capabilityService.findByIds(assignedIds));
    return ListUtils.subtract(changedCapabilityEndpoints, assignedCapabilityEndpoints);
  }

  /**
   * Provides a list of changed endpoints using changed and assigned capability set identifiers.
   *
   * @param changedSetIds - list with changed capability set identifiers
   * @param assignedSetIds - list with assigned capability set identifiers
   * @param excludeList - list with directly assigned capability ids
   * @return list with changed {@link Endpoint} objects
   */
  @Transactional(readOnly = true)
  public List<Endpoint> getByCapabilitySetIds(Collection<UUID> changedSetIds,
    Collection<UUID> assignedSetIds, Collection<Endpoint> excludeList) {
    var changedCapabilities = capabilityService.findByCapabilitySetIds(changedSetIds);
    var assignedCapabilities = capabilityService.findByCapabilitySetIds(assignedSetIds);
    var changedCapabilitySetEndpoints = getCapabilityEndpoints(changedCapabilities);
    var assignedCapabilitySetEndpoints = getCapabilityEndpoints(assignedCapabilities);

    var resultEndpoints = new LinkedHashSet<>(changedCapabilitySetEndpoints);
    assignedCapabilitySetEndpoints.forEach(resultEndpoints::remove);
    excludeList.forEach(resultEndpoints::remove);

    return new ArrayList<>(resultEndpoints);
  }
}
