package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.roles.domain.model.event.DomainEventType.UPDATE;
import static org.folio.roles.utils.CollectionUtils.difference;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.service.permission.PermissionService;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.permission.UserPermissionService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilityEventHandler extends AbstractCapabilityEventHandler {

  private final PolicyService policyService;
  private final CapabilityService capabilityService;
  private final CapabilitySetService capabilitySetService;
  private final RolePermissionService rolePermissionService;
  private final RoleCapabilityService roleCapabilityService;
  private final UserPermissionService userPermissionService;
  private final UserCapabilityService userCapabilityService;

  /**
   * Handles update event for capability.
   *
   * @param event - {@link CapabilityEvent} object
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.domain.model.event.DomainEventType).UPDATE")
  public void handleCapabilityUpdatedEvent(CapabilityEvent event) {
    var newCapability = event.getNewObject();
    var oldCapability = event.getOldObject();
    var capabilityId = newCapability.getId();

    var newCapabilityEndpoints = getCapabilityEndpoints(newCapability);
    var oldCapabilityEndpoints = getCapabilityEndpoints(oldCapability);

    if (Objects.equals(new HashSet<>(newCapabilityEndpoints), new HashSet<>(oldCapabilityEndpoints))) {
      log.debug("Skipping capability event processing, because endpoints not changed: "
        + "capabilityId = {}, eventType = {}", capabilityId, UPDATE);
      return;
    }

    var newEndpoints = difference(newCapabilityEndpoints, oldCapabilityEndpoints);
    var oldEndpoints = difference(oldCapabilityEndpoints, newCapabilityEndpoints);

    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
      performActionForPolicies(
        policyService.findRolePoliciesByCapabilityId(capabilityId),
        AbstractCapabilityEventHandler::extractRoleIds,
        roleId -> updatePermissions(roleId, capabilityId, rolePermissionService, newEndpoints, oldEndpoints));

      performActionForPolicies(
        policyService.findUserPoliciesByCapabilityId(capabilityId),
        AbstractCapabilityEventHandler::extractUserIds,
        userId -> updatePermissions(userId, capabilityId, userPermissionService, newEndpoints, oldEndpoints));
    }
  }

  /**
   * Handles delete event for capability.
   *
   * @param event - {@link CapabilityEvent} object
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.domain.model.event.DomainEventType).DELETE")
  public void handleCapabilityDeletedEvent(CapabilityEvent event) {
    var deprecatedCapability = event.getOldObject();
    var capabilityId = deprecatedCapability.getId();
    var deprecatedEndpoints = getCapabilityEndpoints(deprecatedCapability);

    log.info("Deleting capability {} ", deprecatedCapability.getName());

    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
      performActionForPolicies(
        policyService.findRolePoliciesByCapabilityId(capabilityId),
        AbstractCapabilityEventHandler::extractRoleIds,
        roleId -> deleteDeprecatedPermissions(roleId, capabilityId, rolePermissionService,
          roleCapabilityService::delete, deprecatedEndpoints));

      performActionForPolicies(
        policyService.findUserPoliciesByCapabilityId(capabilityId),
        AbstractCapabilityEventHandler::extractUserIds,
        userId -> deleteDeprecatedPermissions(userId, capabilityId, userPermissionService,
          userCapabilityService::delete, deprecatedEndpoints));

      capabilitySetService.deleteAllLinksToCapability(capabilityId);
      capabilityService.deleteById(capabilityId);
    }
  }

  private void deleteDeprecatedPermissions(UUID identifier, UUID capabilityId, PermissionService permissionService,
    BiConsumer<UUID, UUID> entityCapabilityActionSupplier, List<Endpoint> deprecatedEndpoints) {
    var assignedEndpoints = permissionService.getAssignedEndpoints(identifier, List.of(capabilityId), emptyList());
    var endpointsToDelete = difference(deprecatedEndpoints, assignedEndpoints);
    permissionService.deletePermissions(identifier, endpointsToDelete);
    entityCapabilityActionSupplier.accept(identifier, capabilityId);
  }

  private void updatePermissions(UUID identifier, UUID capabilityId, PermissionService permissionService,
    List<Endpoint> newEndpoints, List<Endpoint> deprecatedEndpoints) {
    var assignedEndpoints = permissionService.getAssignedEndpoints(identifier, List.of(capabilityId), emptyList());
    if (isNotEmpty(newEndpoints)) {
      var endpointsToCreate = difference(newEndpoints, assignedEndpoints);
      permissionService.createPermissions(identifier, endpointsToCreate);
    }

    if (isNotEmpty(deprecatedEndpoints)) {
      var endpointsToDelete = difference(deprecatedEndpoints, assignedEndpoints);
      permissionService.deletePermissions(identifier, endpointsToDelete);
    }
  }

  private static List<Endpoint> getCapabilityEndpoints(Capability capability) {
    return Optional.ofNullable(capability)
      .map(Capability::getEndpoints)
      .orElse(emptyList());
  }
}
