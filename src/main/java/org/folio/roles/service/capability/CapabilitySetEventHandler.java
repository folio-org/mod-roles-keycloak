package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.domain.model.event.DomainEventType.UPDATE;
import static org.folio.roles.utils.CollectionUtils.difference;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.service.permission.PermissionService;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.permission.UserPermissionService;
import org.folio.roles.service.policy.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilitySetEventHandler extends AbstractCapabilityEventHandler {

  private final PolicyService policyService;
  private final CapabilitySetService capabilitySetService;
  private final RoleCapabilitySetService roleCapabilitySetService;
  private final UserCapabilitySetService userCapabilitySetService;
  private final RolePermissionService rolePermissionService;
  private final UserPermissionService userPermissionService;
  private final CapabilityService capabilityService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.domain.model.event.DomainEventType).UPDATE")
  public void handleCapabilitySetUpdatedEvent(CapabilitySetEvent event) {
    var newCapabilitySet = event.getNewObject();
    var oldCapabilitySet = event.getOldObject();
    var id = newCapabilitySet.getId();

    var newCapabilityIds = newCapabilitySet.getCapabilities();
    var oldCapabilityIds = oldCapabilitySet.getCapabilities();

    if (Objects.equals(new HashSet<>(newCapabilityIds), new HashSet<>(oldCapabilityIds))) {
      log.debug("Skipping capability event processing, because capabilities not changed: "
        + "capabilitySetId = {}, eventType = {}", id, UPDATE);
      return;
    }

    var oldCapabilitySetEndpoints = getCapabilitySetEndpoints(oldCapabilitySet);
    var newCapabilitySetEndpoints = getCapabilitySetEndpoints(newCapabilitySet);
    var deprecatedEndpoints = difference(oldCapabilitySetEndpoints, newCapabilitySetEndpoints);

    var newCapabilities = difference(newCapabilityIds, oldCapabilityIds);
    performActionForPolicies(
      policyService.findRolePoliciesByCapabilitySetId(id),
      AbstractCapabilityEventHandler::extractRoleIds,
      roleId -> updatePermissions(roleId, id, rolePermissionService, newCapabilities, deprecatedEndpoints));

    performActionForPolicies(
      policyService.findUserPoliciesByCapabilitySetId(id),
      AbstractCapabilityEventHandler::extractUserIds,
      userId -> updatePermissions(userId, id, userPermissionService, newCapabilities, deprecatedEndpoints));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.domain.model.event.DomainEventType).DELETE")
  public void handleCapabilitySetDeletedEvent(CapabilitySetEvent event) {
    var deprecatedCapabilitySet = event.getOldObject();
    var capabilitySetId = deprecatedCapabilitySet.getId();

    performActionForPolicies(
      policyService.findRolePoliciesByCapabilitySetId(capabilitySetId),
      AbstractCapabilityEventHandler::extractRoleIds,
      roleId -> roleCapabilitySetService.delete(roleId, capabilitySetId));

    performActionForPolicies(
      policyService.findUserPoliciesByCapabilitySetId(capabilitySetId),
      AbstractCapabilityEventHandler::extractUserIds,
      userId -> userCapabilitySetService.delete(userId, capabilitySetId));

    capabilitySetService.deleteById(capabilitySetId);
  }

  private void updatePermissions(UUID identifier, UUID capabilitySetId, PermissionService permissionService,
    List<UUID> newCapabilityIds, List<Endpoint> deprecatedEndpoints) {
    if (isNotEmpty(newCapabilityIds)) {
      var newEndpoints = getCapabilityEndpoints(capabilityService.findByIds(newCapabilityIds));
      permissionService.createPermissions(identifier, newEndpoints);
    }

    if (isNotEmpty(deprecatedEndpoints)) {
      var assignedEndpoints = permissionService.getAssignedEndpoints(identifier, emptyList(), List.of(capabilitySetId));
      var deprecatedValues = difference(deprecatedEndpoints, assignedEndpoints);
      permissionService.deletePermissions(identifier, deprecatedValues);
    }
  }

  private static List<Endpoint> getCapabilityEndpoints(List<Capability> capabilities) {
    return toStream(capabilities)
      .map(Capability::getEndpoints)
      .flatMap(Collection::stream)
      .distinct()
      .toList();
  }

  private static List<Endpoint> getCapabilitySetEndpoints(ExtendedCapabilitySet capabilitySet) {
    return Optional.ofNullable(capabilitySet.getCapabilityList())
      .stream()
      .flatMap(Collection::stream)
      .map(Capability::getEndpoints)
      .flatMap(Collection::stream)
      .distinct()
      .toList();
  }
}
