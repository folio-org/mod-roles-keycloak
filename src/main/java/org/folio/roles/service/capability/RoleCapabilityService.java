package org.folio.roles.service.capability;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapability;
import org.folio.roles.domain.model.PageResult;

public interface RoleCapabilityService {

  /**
   * Creates a record(s) associating one or more capabilities with the role.
   *
   * @param roleId - role identifier as {@link UUID} object
   * @param capabilityIds - capability identifiers as {@link List} of {@link UUID} objects
   * @param safeCreate - defines if new capabilities must be added or error thrown if any already exists
   * @return {@link RoleCapability} object with created role-capability relations
   */
  PageResult<RoleCapability> create(UUID roleId, List<UUID> capabilityIds, boolean safeCreate);

  PageResult<RoleCapability> create(RoleCapabilitiesRequest roleCapabilitiesRequest, boolean safeCreate);


  /**
   * Retrieves role-capability items by CQL query.
   *
   * @param query - CQL query as {@link String} object
   * @param limit - a number of results in response
   * @param offset - offset in pagination from first record.
   * @return {@link PageResult} object with found {@link RoleCapability} relation
   *   descriptors.
   */
  PageResult<RoleCapability> find(String query, Integer limit, Integer offset);

  /**
   * Updates role-capability relations.
   *
   * @param roleId - role identifier as {@link UUID} object
   * @param capabilityIds - list of capabilities that must be assigned to a role
   */
  void update(UUID roleId, List<UUID> capabilityIds);

  /**
   * Removes role-capability relations by role identifier.
   *
   * @param roleId - role identifier as {@link UUID}
   * @throws EntityNotFoundException if role is not found by id or there is no assigned values
   */
  void delete(UUID roleId, UUID capabilityId);

  /**
   * Removes role-capability relations by role identifier and capability ids.
   *
   * @param roleId - role identifier as {@link UUID}
   * @param capabilityIds - list with capabilities, that should be removed from a role
   */
  void delete(UUID roleId, List<UUID> capabilityIds);

  /**
   * Removes role-capability relations by role identifier.
   *
   * @param roleId - role identifier as {@link UUID}
   * @throws EntityNotFoundException if role is not found by id or there is no assigned values
   */
  void deleteAll(UUID roleId);

  /**
   * Provides capability ids associated with role through capability sets.
   *
   * @param roleId - role identifier
   * @return {@link List} with capability {@link UUID} identifiers
   */
  List<UUID> getCapabilitySetCapabilityIds(UUID roleId);
}
