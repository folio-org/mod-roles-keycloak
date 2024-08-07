package org.folio.roles.service.permission;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.service.permission.PermissionService.convertToString;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
import org.folio.roles.service.capability.CapabilityEndpointService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.service.role.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class RolePermissionService implements PermissionService {

  private final RoleService roleService;
  private final PolicyService policyService;
  private final CapabilityEndpointService capabilityEndpointService;
  private final KeycloakAuthorizationService keycloakAuthService;

  @Override
  @Transactional
  public void createPermissions(UUID roleId, List<Endpoint> endpoints) {
    if (isEmpty(endpoints)) {
      return;
    }

    log.info("Creating permissions for role: id = {}, endpoints = {}", () -> roleId, () -> convertToString(endpoints));
    var role = roleService.getById(roleId);
    var policyName = getPolicyName(role.getId());
    var policy = policyService.getOrCreatePolicy(policyName, ROLE, () -> createNewRolePolicy(roleId));
    keycloakAuthService.createPermissions(policy, endpoints, getPermissionNameGenerator(roleId));
  }

  @Override
  @Transactional
  public void deletePermissions(UUID roleId, List<Endpoint> endpoints) {
    if (isEmpty(endpoints)) {
      return;
    }

    log.debug("Removing permissions for role: id = {}, endpoints = {}", () -> roleId, () -> convertToString(endpoints));
    var role = roleService.getById(roleId);
    var policyName = getPolicyName(role.getId());
    var policy = policyService.getByNameAndType(policyName, ROLE);
    keycloakAuthService.deletePermissions(policy, endpoints, getPermissionNameGenerator(roleId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Endpoint> getAssignedEndpoints(UUID roleId, List<UUID> excludedCapabilityIds, List<UUID> excludedSetIds) {
    return capabilityEndpointService.getRoleAssignedEndpoints(roleId, excludedCapabilityIds, excludedSetIds);
  }

  public static Function<Endpoint, String> getPermissionNameGenerator(UUID roleId) {
    return endpoint -> format("%s access for role '%s' to '%s'", endpoint.getMethod(), roleId, endpoint.getPath());
  }

  private static String getPolicyName(UUID roleId) {
    return "Policy for role: " + roleId;
  }

  private static Policy createNewRolePolicy(UUID roleId) {
    return new Policy()
      .type(ROLE)
      .source(SourceType.SYSTEM)
      .name(getPolicyName(roleId))
      .description("System generated policy for role: " + roleId)
      .rolePolicy(new RolePolicy().addRolesItem(new RolePolicyRole().id(roleId)));
  }
}
