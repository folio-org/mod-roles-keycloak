package org.folio.roles.service.loadablerole;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.LoadablePermission;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.event.DomainEvent;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class LoadableRoleCapabilityAssignmentProcessor {

  private final LoadablePermissionService service;
  private final RoleCapabilityService roleCapabilityService;

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitiesCreatedEvent(DomainEvent<? extends Collection<Capability>> event) {
    log.debug("\"Capabilities Created\" event received: {}", event);

    var capabilities = event.getNewObject();
    if (isEmpty(capabilities)) {
      throw new IllegalArgumentException("Capabilities Created event contains no data");
    }

    log.info("Handling created capabilities: {}", () -> toNames(capabilities));

    try (var ignored = new FolioExecutionContextSetter(event.getContext())) {
      var capabilityByPerm = capabilities.stream().collect(toMap(Capability::getPermission, identity()));

      var roleIdWithPermissions = service.findAllByPermissions(capabilityByPerm.keySet()).stream()
        .collect(groupingBy(LoadablePermission::getRoleId));

      roleIdWithPermissions.forEach(assignCapabilitiesToRole(capabilityByPerm));
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

  private BiConsumer<UUID, List<LoadablePermission>> assignCapabilitiesToRole(Map<String, Capability> capabilityByPerm) {
    return (roleId, rolePermissions) -> {
      var capabilityIds = selectCapabilityIdsByRolePermissions(capabilityByPerm, rolePermissions);

      roleCapabilityService.create(roleId, capabilityIds);

      rolePermissions.forEach(assignCapabilityId(capabilityByPerm));
      service.saveAll(rolePermissions);
    };
  }

  private static List<UUID> selectCapabilityIdsByRolePermissions(Map<String, Capability> capabilityByPerm,
    List<LoadablePermission> rolePermissions) {
    return rolePermissions.stream()
      .map(LoadablePermission::getPermissionName)
      .map(capabilityByPerm::get)
      .map(Capability::getId)
      .toList();
  }

  private static Consumer<LoadablePermission> assignCapabilityId(Map<String, Capability> capabilityByPerm) {
    return perm -> {
      var capability = capabilityByPerm.get(perm.getPermissionName());
      perm.setCapabilityId(capability.getId());
    };
  }
}
