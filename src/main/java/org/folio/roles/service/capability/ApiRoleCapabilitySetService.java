package org.folio.roles.service.capability;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.RoleCapabilitySet;
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
  public PageResult<RoleCapabilitySet> create(UUID roleId, List<UUID> capabilitySetIds) {
    checkRoleIsNotDefault(roleId);
    return delegate.create(roleId, capabilitySetIds);
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
}
