package org.folio.roles.service.permission;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.roles.domain.dto.Endpoint;

public interface PermissionService {

  /**
   * Creates authorization permissions based on entity identifier and list of capability ids.
   *
   * @param id - entity identifier as {@link UUID}
   * @param endpoints - list with assigned capabilities to skip
   */
  void createPermissions(UUID id, List<Endpoint> endpoints);

  /**
   * Deletes authorization permissions based on entity identifier and list of capability ids.
   *
   * @param id - entity identifier as {@link UUID}
   * @param endpoints - list with assigned capability identifiers
   */
  void deletePermissions(UUID id, List<Endpoint> endpoints);

  List<Endpoint> getAssignedEndpoints(UUID id, List<UUID> excludedCapabilityIds, List<UUID> excludedCapabilitySetIds);

  /**
   * Represents a list with {@link Endpoint} values in human-readable format.
   *
   * @param endpoints - list with endpoints to process
   * @return list of endpoints as human-readable {@link String} representation
   */
  static String convertToString(List<Endpoint> endpoints) {
    return endpoints.stream()
      .map(endpoint -> String.format("{path: %s, method: %s}", endpoint.getPath(), endpoint.getMethod()))
      .collect(Collectors.joining(", ", "[", "]"));
  }
}
