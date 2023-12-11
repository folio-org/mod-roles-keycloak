package org.folio.roles.service.permission;

import static java.lang.String.format;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.service.permission.PermissionService.convertToString;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
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
  private final KeycloakAuthorizationService keycloakAuthService;

  @Override
  @Transactional
  public void createPermissions(UUID roleId, List<Endpoint> endpoints) {
    log.info("Creating permissions for role: id = {}, endpoints = {}", () -> roleId, () -> convertToString(endpoints));
    var role = roleService.getById(roleId);
    var policyName = getPolicyName(role.getName());
    var policy = policyService.getOrCreatePolicy(policyName, ROLE, () -> createNewRolePolicy(role));
    keycloakAuthService.createPermissions(policy, endpoints, getPermissionNameGenerator(role.getName()));
  }

  @Override
  @Transactional
  public void deletePermissions(UUID roleId, List<Endpoint> endpoints) {
    log.debug("Removing permissions for role: id = {}, endpoints = {}", () -> roleId, () -> convertToString(endpoints));
    var role = roleService.getById(roleId);
    var policyName = getPolicyName(role.getName());
    var policy = policyService.getByNameAndType(policyName, ROLE);
    keycloakAuthService.deletePermissions(policy, endpoints, getPermissionNameGenerator(role.getName()));
  }

  private static Function<Endpoint, String> getPermissionNameGenerator(String roleName) {
    return endpoint -> format("%s access for role '%s' to '%s'", endpoint.getMethod(), roleName, endpoint.getPath());
  }

  private static Policy createNewRolePolicy(Role role) {
    return new Policy()
      .type(ROLE)
      .name(getPolicyName(role.getName()))
      .description("System generated policy for role: " + role.getName())
      .rolePolicy(new RolePolicy().addRolesItem(new RolePolicyRole().id(role.getId())));
  }

  private static String getPolicyName(String roleName) {
    return "Policy for role: " + roleName;
  }
}
