package org.folio.roles.service.loadablerole;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.utils.CollectionUtils.mapItemsToSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.LoadablePermission;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.event.DomainEvent;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoadableRoleCapabilityAssignmentProcessor {

  private final LoadableRoleService service;
  private final RoleCapabilityService roleCapabilityService;

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitiesCreatedEvent(DomainEvent<? extends Collection<Capability>> event) {
    log.debug("\"Capabilities Created\" event received: {}", event);

    var capabilities = event.getNewObject();
    if (isEmpty(capabilities)) {
      log.warn("Capabilities Created event contains no data");
    }

    log.info("Handling created capabilities: {}", () -> toNames(capabilities));

    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
      var capabilityPerms = mapItemsToSet(capabilities, Capability::getPermission);

      var roles = service.findAllByPermissions(capabilityPerms);

      for (LoadableRole role : roles) {
        var rolePerms = mapItemsToSet(role.getPermissions(), LoadablePermission::getPermissionName);
        var capabilityIds = selectCapabilityIdsByRolePermissions(capabilities, rolePerms);

        roleCapabilityService.create(role.getId(), capabilityIds);
      }
    }
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitySetCreatedEvent(DomainEvent<CapabilitySet> event) {
    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
    }
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).UPDATE")
  public void handleCapabilitySetUpdatedEvent(DomainEvent<CapabilitySet> event) {
    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
    }
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).DELETE")
  public void handleCapabilitySetDeletedEvent(DomainEvent<CapabilitySet> event) {
    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
    }
  }

  private static String toNames(Collection<Capability> capabilities) {
    return toStream(capabilities).map(Capability::getName).collect(Collectors.joining(", "));
  }

  private static List<UUID> selectCapabilityIdsByRolePermissions(Collection<Capability> capabilities,
    Set<String> rolePerms) {
    return capabilities.stream()
      .filter(capability -> rolePerms.contains(capability.getPermission()))
      .map(Capability::getId)
      .toList();
  }
}
