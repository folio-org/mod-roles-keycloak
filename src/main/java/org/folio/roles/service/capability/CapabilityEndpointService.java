package org.folio.roles.service.capability;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityEndpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.entity.CapabilityEndpointEntity;
import org.folio.roles.repository.CapabilityEndpointRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CapabilityEndpointService {

  private final CapabilityService capabilityService;
  private final CapabilityEndpointRepository capabilityEndpointRepository;

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

  /**
   * Retrieves endpoints that was assigned to the role excluding assignment through capability and capability set ids.
   *
   * @param roleId - role identifier
   * @param exclCapabilityIds - excluded capability identifiers for search request
   * @param exclSetIds - excluded capability set identifiers for search request
   * @return list with endpoints that was assigned to the role
   */
  @Transactional(readOnly = true)
  public List<Endpoint> getRoleAssignedEndpoints(UUID roleId, List<UUID> exclCapabilityIds, List<UUID> exclSetIds) {
    var capabilityIdsString = StringUtils.join(nullIfEmpty(exclCapabilityIds), ",");
    var setIdsString = StringUtils.join(nullIfEmpty(exclSetIds), ",");
    var capabilityEndpoints = capabilityEndpointRepository.getByRoleId(roleId, capabilityIdsString);
    var capabilitySetEndpoints = capabilityEndpointRepository.getByRoleId(roleId, capabilityIdsString, setIdsString);
    return getEndpoints(ListUtils.union(capabilityEndpoints, capabilitySetEndpoints));
  }

  /**
   * Retrieves endpoints that was assigned to the user excluding assignment through capability and capability set ids.
   *
   * @param userId - user identifier
   * @param exclCapabilityIds - excluded capability identifiers for search request
   * @param exclSetIds - excluded capability set identifiers for search request
   * @return list with endpoints that was assigned to the role
   */
  @Transactional(readOnly = true)
  public List<Endpoint> getUserAssignedEndpoints(UUID userId, List<UUID> exclCapabilityIds, List<UUID> exclSetIds) {
    var capabilityIdsString = StringUtils.join(nullIfEmpty(exclCapabilityIds), ",");
    var setIdsString = StringUtils.join(nullIfEmpty(exclSetIds), ",");
    var capabilityEndpoints = capabilityEndpointRepository.getByUserId(userId, capabilityIdsString);
    var capabilitySetEndpoints = capabilityEndpointRepository.getByUserId(userId, capabilityIdsString, setIdsString);
    return getEndpoints(ListUtils.union(capabilityEndpoints, capabilitySetEndpoints));
  }

  private static List<Endpoint> getEndpoints(List<CapabilityEndpointEntity> endpointEntities) {
    return toStream(endpointEntities)
      .map(entity -> new Endpoint().path(entity.getPath()).method(entity.getMethod()))
      .distinct()
      .toList();
  }

  private static <T> List<T> nullIfEmpty(List<T> list) {
    return isEmpty(list) ? null : list;
  }
}
