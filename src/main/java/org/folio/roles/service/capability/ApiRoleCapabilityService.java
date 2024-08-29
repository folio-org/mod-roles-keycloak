package org.folio.roles.service.capability;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.RoleCapability;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.springframework.stereotype.Service;

@Log4j2
@Service("apiRoleCapabilityService")
@RequiredArgsConstructor
public class ApiRoleCapabilityService implements RoleCapabilityService {

  private final RoleCapabilityService delegate;
  private final LoadableRoleService loadableRoleService;

  @Override
  public PageResult<RoleCapability> create(UUID roleId, List<UUID> capabilityIds, boolean safeCreate) {
    checkRoleIsNotDefault(roleId);
    return delegate.create(roleId, capabilityIds, safeCreate);
  }

  @Override
  public PageResult<RoleCapability> create(RoleCapabilitiesRequest request, boolean safeCreate) {
    checkRoleIsNotDefault(request.getRoleId());
    return delegate.create(request, safeCreate);
  }

  @Override
  public PageResult<RoleCapability> find(String query, Integer limit, Integer offset) {
    return delegate.find(query, limit, offset);
  }

  @Override
  public void update(UUID roleId, List<UUID> capabilityIds) {
    checkRoleIsNotDefault(roleId);
    delegate.update(roleId, capabilityIds);
  }

  @Override
  public void delete(UUID roleId, UUID capabilityId) {
    checkRoleIsNotDefault(roleId);
    delegate.delete(roleId, capabilityId);
  }

  @Override
  public void delete(UUID roleId, List<UUID> capabilityIds) {
    checkRoleIsNotDefault(roleId);
    delegate.delete(roleId, capabilityIds);
  }

  @Override
  public void deleteAll(UUID roleId) {
    checkRoleIsNotDefault(roleId);
    delegate.deleteAll(roleId);
  }

  @Override
  public List<UUID> getCapabilitySetCapabilityIds(UUID roleId) {
    return delegate.getCapabilitySetCapabilityIds(roleId);
  }

  private void checkRoleIsNotDefault(UUID roleId) {
    if (loadableRoleService.isDefaultRole(roleId)) {
      throw new ServiceException("Changes to default role are prohibited: roleId = " + roleId,
        "roleId", roleId.toString());
    }
  }
}
