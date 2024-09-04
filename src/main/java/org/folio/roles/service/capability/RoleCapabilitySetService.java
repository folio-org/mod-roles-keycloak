package org.folio.roles.service.capability;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitySet;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.model.PageResult;

public interface RoleCapabilitySetService {

  /**
   * Creates a record(s) associating one or more capabilitySets with a role.
   *
   * @param roleId - role identifier as {@link UUID} object
   * @param capabilitySetIds - capabilitySet identifiers as {@link List} of {@link UUID} objects
   * @param safeCreate - defines if new capabilities must be added or error thrown if any already exists
   * @return {@link PageResult} with created {@link RoleCapabilitySet} relations
   */
  PageResult<RoleCapabilitySet> create(UUID roleId, List<UUID> capabilitySetIds, boolean safeCreate);

  /**
   * Creates a record(s) associating one or more capabilitySets with a role.
   *
   * @param request - RoleCapabilitySetsRequest contains roleId, capabilitySetIds, and capabilitySetNames
   * @param safeCreate - defines if new capabilities must be added or error thrown if any already exists
   * @return {@link PageResult} with created {@link RoleCapabilitySet} relations
   */
  PageResult<RoleCapabilitySet> create(RoleCapabilitySetsRequest request, boolean safeCreate);

  /**
   * Retrieves role-capabilitySets items by CQL query.
   *
   * @param query - CQL query as {@link String} object
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} with found {@link RoleCapabilitySet} relations
   */
  PageResult<RoleCapabilitySet> find(String query, Integer limit, Integer offset);

  /**
   * Updates a list of assigned to a role capabilitySets.
   *
   * @param roleId - role identifier
   * @param capabilityIds - list with new capabilitySets, that should be assigned to a role
   */
  void update(UUID roleId, List<UUID> capabilityIds);

  /**
   * Updates a list of assigned to a role capabilitySets.
   *
   * @param roleId - role identifier as {@link UUID} object
   * @param request - CapabilitySetsUpdateRequest that contains either capability IDs or names, to be assigned to a role
   */
  void update(UUID roleId, CapabilitySetsUpdateRequest request);

  /**
   * Removes role assigned capabilities using role identifier.
   *
   * @param roleId - role identifier as {@link UUID}
   * @throws jakarta.persistence.EntityNotFoundException if role is not found by id or there is no assigned values
   */
  void deleteAll(UUID roleId);

  /**
   * Removes role assigned capability set using role identifier and capability set id.
   *
   * @param roleId - role identifier as {@link UUID}
   * @param capabilitySetId - capability set identifier as {@link UUID}
   * @throws jakarta.persistence.EntityNotFoundException if assignment is not found by role id and capability set id
   */
  void delete(UUID roleId, UUID capabilitySetId);

  /**
   * Removes role assigned capability sets using role identifier and capability set ids.
   *
   * @param roleId - role identifier as {@link UUID}
   * @param capabilitySetIds - list with capabilitySet ids, that should be removed from a role
   */
  void delete(UUID roleId, List<UUID> capabilitySetIds);
}
