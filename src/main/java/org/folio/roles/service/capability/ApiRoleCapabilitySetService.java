package org.folio.roles.service.capability;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitySet;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.springframework.stereotype.Service;

@Log4j2
@Service("apiRoleCapabilitySetService")
@RequiredArgsConstructor
public class ApiRoleCapabilitySetService implements RoleCapabilitySetService {

  private final RoleCapabilitySetService delegate;
  private final LoadableRoleService loadableRoleService;

  @Override
  public PageResult<RoleCapabilitySet> create(UUID roleId, List<UUID> capabilitySetIds, boolean safeCreate) {
    checkRoleIsNotDefault(roleId);
    return delegate.create(roleId, capabilitySetIds, safeCreate);
  }

  @Override
  public PageResult<RoleCapabilitySet> create(RoleCapabilitySetsRequest request, boolean safeCreate) {
    checkRoleIsNotDefault(request.getRoleId());
    verifyCapabilitySets(request.getCapabilitySetIds(), request.getCapabilitySetNames());
    return delegate.create(request, safeCreate);
  }

  @Override
  public PageResult<RoleCapabilitySet> find(String query, Integer limit, Integer offset) {
    return delegate.find(query, limit, offset);
  }

  @Override
  public void update(UUID roleId, List<UUID> capabilityIds) {
    checkRoleIsNotDefault(roleId);
    delegate.update(roleId, capabilityIds);
  }

  @Override
  public void update(UUID roleId, CapabilitySetsUpdateRequest request) {
    checkRoleIsNotDefault(roleId);
    delegate.update(roleId, request);
  }

  @Override
  public void deleteAll(UUID roleId) {
    checkRoleIsNotDefault(roleId);
    delegate.deleteAll(roleId);
  }

  @Override
  public void delete(UUID roleId, UUID capabilitySetId) {
    checkRoleIsNotDefault(roleId);
    delegate.delete(roleId, capabilitySetId);
  }

  @Override
  public void delete(UUID roleId, List<UUID> capabilitySetIds) {
    checkRoleIsNotDefault(roleId);
    delegate.delete(roleId, capabilitySetIds);
  }

  private void checkRoleIsNotDefault(UUID roleId) {
    if (loadableRoleService.isDefaultRole(roleId)) {
      throw new ServiceException("Changes to default role are prohibited: roleId = " + roleId,
        "roleId", roleId.toString());
    }
  }

  private void verifyCapabilitySets(List<UUID> capabilitySetIds, List<String> capabilitySetNames) {
    if (isEmpty(capabilitySetIds) && isEmpty(capabilitySetNames)) {
      throw new IllegalArgumentException("'capabilitySetIds' or 'capabilitySetNames' must not be null");
    }
  }
}
