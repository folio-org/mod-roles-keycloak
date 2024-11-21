package org.folio.roles.service.permission;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.roles.domain.dto.PolicyType.USER;
import static org.folio.roles.integration.keyclock.KeycloakUserService.getUserId;
import static org.folio.roles.service.permission.PermissionService.convertToString;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.domain.dto.UserPolicy;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.service.capability.CapabilityEndpointService;
import org.folio.roles.service.policy.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserPermissionService implements PermissionService {

  private final PolicyService policyService;
  private final KeycloakUserService keycloakUserService;
  private final CapabilityEndpointService capabilityEndpointService;
  private final KeycloakAuthorizationService keycloakAuthService;

  @Override
  @Transactional
  public void createPermissions(UUID userId, List<Endpoint> endpoints) {

    log.info("Creating permissions for user: id = {}, endpoints = {}", () -> userId, () -> convertToString(endpoints));
    var kcUser = keycloakUserService.getKeycloakUserByUserId(userId);
    var policyName = getPolicyName(getUserId(kcUser));
    var userPolicy = policyService.getOrCreatePolicy(policyName, USER, () -> createNewUserPolicy(userId));
    keycloakAuthService.createPermissions(userPolicy, endpoints, getPermissionNameGenerator(userId));
  }

  @Override
  @Transactional
  public void deletePermissions(UUID userId, List<Endpoint> endpoints) {
    if (isEmpty(endpoints)) {
      return;
    }

    log.debug("Removing permissions for user: id = {}, endpoints = {}", () -> userId, () -> convertToString(endpoints));
    var kcUser = keycloakUserService.getKeycloakUserByUserId(userId);
    var folioUserId = getUserId(kcUser);
    var policyName = getPolicyName(folioUserId);
    var policy = policyService.getByNameAndType(policyName, USER);
    keycloakAuthService.deletePermissions(policy, endpoints, getPermissionNameGenerator(folioUserId));
  }

  @Override
  public List<Endpoint> getAssignedEndpoints(UUID userId, List<UUID> excludedCapabilityIds, List<UUID> excludedSetIds) {
    return capabilityEndpointService.getUserAssignedEndpoints(userId, excludedCapabilityIds, excludedSetIds);
  }

  private static Function<Endpoint, String> getPermissionNameGenerator(UUID userId) {
    return endpoint -> format("%s access for user '%s' to '%s'", endpoint.getMethod(), userId, endpoint.getPath());
  }

  private static String getPolicyName(UUID userId) {
    return "Policy for user: " + userId;
  }

  private static Policy createNewUserPolicy(UUID userId) {
    return new Policy()
      .type(USER)
      .source(SourceType.SYSTEM)
      .name(getPolicyName(userId))
      .description("System generated policy for user: " + userId)
      .userPolicy(new UserPolicy().users(List.of(userId)));
  }
}
