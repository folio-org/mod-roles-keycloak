package org.folio.roles.service.loadablerole;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.utils.CollectionUtils.findOne;

import jakarta.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.LoadablePermission;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.service.event.CapabilityCollectionEvent;
import org.folio.roles.service.event.CapabilitySetEvent;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class LoadableRoleCapabilityAssignmentProcessor {

  private final LoadablePermissionService service;
  private final CapabilityService capabilityService;
  private final RoleCapabilityService roleCapabilityService;
  private final RoleCapabilitySetService roleCapabilitySetService;

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitiesCreatedEvent(CapabilityCollectionEvent<? extends Collection<Capability>> event) {
    log.debug("\"Capabilities Created\" event received: {}", event);

    var capabilities = event.getNewObject();
    if (isEmpty(capabilities)) {
      throw new IllegalArgumentException("Capabilities Created event contains no data");
    }

    log.info("Handling created capabilities: {}", () -> toNames(capabilities));

    var capabilityByPerm = capabilities.stream().collect(toMap(Capability::getPermission, identity()));

    applyActionToRoles(() -> findRoleWithPermissionsByPermissionNames(capabilityByPerm.keySet()),
      assignCapabilitiesToRole(capabilityByPerm),
      event.getContext());
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitySetCreatedEvent(CapabilitySetEvent event) {
    log.debug("\"Capability Set Created\" event received: {}", event);

    var capabilitySet = event.getNewObject();
    log.info("Handling created capability set: {}", () -> shortDescription(capabilitySet));

    applyActionToRoles(findRolesWithPermissionsByCapabilitySet(capabilitySet),
      assignCapabilitySetToRole(capabilitySet),
      event.getContext());
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).UPDATE")
  public void handleCapabilitySetUpdatedEvent(CapabilitySetEvent event) {
    log.debug("\"Capability Set Updated\" event received: {}", event);

    var capabilitySet = event.getNewObject();
    var oldCapabilitySet = event.getOldObject();
    
    if (capabilitySet.getId() != oldCapabilitySet.getId()) {
      throw new IllegalArgumentException("Ids of old and new version of capability set don't match: oldId = "
        + oldCapabilitySet.getId() + ", newId = " + capabilitySet.getId());
    }

    log.info("Handling updated capability set: {}", () -> shortDescription(capabilitySet));

    // reflecting changes in the roles due to modification of a capability set currently is not supported
    // by Role Capability Set service
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).DELETE")
  public void handleCapabilitySetDeletedEvent(CapabilitySetEvent event) {
    log.debug("\"Capability Set Deleted\" event received: {}", event);

    var capabilitySet = event.getOldObject();
    log.info("Handling deleted capability set: {}", () -> shortDescription(capabilitySet));

    applyActionToRoles(findRolesWithPermissionsByCapabilitySet(capabilitySet),
      deleteCapabilitySetFromRole(capabilitySet),
      event.getContext());
  }

  private Supplier<Map<UUID, List<LoadablePermission>>> findRolesWithPermissionsByCapabilitySet(
    CapabilitySet capabilitySet) {
    return () -> {
      var relatedCapability = getCapabilityByCapabilitySetName(capabilitySet.getName());
      log.debug("Capability found by capability set name: {}", relatedCapability);

      var capabilityPerm = relatedCapability.getPermission();

      return findRoleWithPermissionsByPermissionNames(List.of(capabilityPerm));
    };
  }

  private Map<UUID, List<LoadablePermission>> findRoleWithPermissionsByPermissionNames(
    Collection<String> permissions) {
    return service.findAllByPermissions(permissions)
      .stream()
      .collect(groupingBy(LoadablePermission::getRoleId));
  }

  private BiConsumer<UUID, List<LoadablePermission>> assignCapabilitiesToRole(
    Map<String, Capability> capabilityByPerm) {
    return (roleId, rolePermissions) -> {
      log.info("Assigning capabilities to loadable role: roleId = {}, affectedPermissions = {}", () -> roleId,
        () -> toPermissionNames(rolePermissions));

      var capabilityIds = selectCapabilityIdsByRolePermissions(capabilityByPerm, rolePermissions);

      roleCapabilityService.create(roleId, capabilityIds);

      rolePermissions.forEach(assignCapabilityId(capabilityByPerm));

      service.saveAll(rolePermissions);
      log.debug("Loadable permissions saved with assigned capabilities");
    };
  }

  private BiConsumer<UUID, List<LoadablePermission>> assignCapabilitySetToRole(CapabilitySet capabilitySet) {
    return (roleId, rolePermissions) -> {
      var rolePermission = getSingleLoadablePermission(roleId, rolePermissions);

      roleCapabilitySetService.create(roleId, List.of(capabilitySet.getId()));

      rolePermission.setCapabilitySetId(capabilitySet.getId());
      service.save(rolePermission);

      log.info("Capability set assigned to loadable role: roleId = {}, permission = {}, capabilitySet = {}",
        () -> roleId, () -> rolePermission, () -> shortDescription(capabilitySet));
    };
  }

  private BiConsumer<UUID, List<LoadablePermission>> deleteCapabilitySetFromRole(CapabilitySet capabilitySet) {
    return (roleId, rolePermissions) -> {
      var rolePermission = getSingleLoadablePermission(roleId, rolePermissions);

      roleCapabilitySetService.delete(roleId, capabilitySet.getId());

      rolePermission.setCapabilitySetId(null);
      service.save(rolePermission);

      log.info("Capability set removed from loadable role: roleId = {}, permission = {}, capabilitySet = {}",
        () -> roleId, () -> rolePermission, () -> shortDescription(capabilitySet));
    };
  }

  private Capability getCapabilityByCapabilitySetName(String capabilitySetName) {
    var capability = findOne(capabilityService.findByNames(List.of(capabilitySetName)));
    return capability.orElseThrow(
      () -> new EntityNotFoundException("Single capability is not found by capability set name: " + capabilitySetName));
  }

  private static void applyActionToRoles(Supplier<Map<UUID, List<LoadablePermission>>> rolesWithPermissionsSupplier,
    BiConsumer<UUID, List<LoadablePermission>> action, FolioExecutionContext context) {
    try (var ignored = new FolioExecutionContextSetter(context)) {
      var roleIdWithPermissions = rolesWithPermissionsSupplier.get();

      log.debug("Action will be applied to the following roles/permissions: {}", roleIdWithPermissions);

      roleIdWithPermissions.forEach(action);
    }
  }

  private static LoadablePermission getSingleLoadablePermission(UUID roleId, List<LoadablePermission> rolePermissions) {
    if (rolePermissions.size() != 1) {
      throw new IllegalStateException("Expected one loadable permission in the role but several found: "
        + "roleId = " + roleId + ", rolePermissions = " + rolePermissions);
    }
    return rolePermissions.get(0);
  }

  private static Collection<String> toNames(Collection<Capability> capabilities) {
    return mapItems(capabilities, Capability::getName);
  }

  private static Collection<String> toPermissionNames(Collection<LoadablePermission> rolePermissions) {
    return mapItems(rolePermissions, LoadablePermission::getPermissionName);
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
      String permissionName = perm.getPermissionName();
      var capability = capabilityByPerm.get(permissionName);

      UUID capabilityId = capability.getId();
      perm.setCapabilityId(capabilityId);

      log.debug("Capability assigned to loadable permission: roleId = {}, permission = {}, capabilityId = {}",
        perm.getRoleId(), permissionName, capabilityId);
    };
  }

  private static String shortDescription(CapabilitySet set) {
    return new ToStringBuilder(set)
      .append("id", set.getId())
      .append("name", set.getName())
      .append("applicationId", set.getApplicationId())
      .build();
  }
}
