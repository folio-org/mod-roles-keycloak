package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.domain.model.event.CapabilitySetEvent.deleted;

import jakarta.persistence.EntityExistsException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.folio.roles.domain.model.CapabilityReplacements;
import org.folio.roles.domain.model.event.DomainEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CapabilityReplacementsService {

  private final FolioExecutionContext folioExecutionContext;
  private final RoleCapabilityRepository roleCapabilityRepository;
  private final RoleCapabilitySetRepository roleCapabilitySetRepository;
  private final UserCapabilityRepository userCapabilityRepository;
  private final UserCapabilitySetRepository userCapabilitySetRepository;
  private final UserCapabilityService userCapabilityService;
  private final UserCapabilitySetService userCapabilitySetService;
  private final RoleCapabilityService roleCapabilityService;
  private final RoleCapabilitySetService roleCapabilitySetService;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final CapabilityService capabilityService;
  private final CapabilitySetService capabilitySetService;
  private final CapabilitySetMapper capabilitySetMapper;
  private final LoadablePermissionRepository loadablePermissionRepository;

  /**
   * Process permission replacements.
   *
   * @param capabilityReplacements a map of old to new permissions, e.g. {"old" : ["new1", "new2"]}
   */
  @Transactional
  public void processReplacements(CapabilityReplacements capabilityReplacements) {
    log.info("Processing assignments of replacement capabilities.");
    assignReplacementCapabilities(capabilityReplacements);
    log.info("Processing replacement of loadable capabilities.");
    replaceLoadable(capabilityReplacements);
    log.info("Processing unassigning of replaced capabilities.");
    unassignReplacedCapabilities(capabilityReplacements);
    log.info("Finished processing capability replacements.");
  }

  public Optional<CapabilityReplacements> deduceReplacements(CapabilityEvent newValue) {
    if (newValue == null || newValue.getResources() == null) {
      return empty();
    }

    var permissionReplacements =
      newValue.getResources().stream()
        .map(FolioResource::getPermission)
        .filter(Objects::nonNull)
        .flatMap(CapabilityReplacementsService::mapToReplacesByPermission)
        .filter(entry -> !entry.getKey().equals(entry.getValue()))
        .collect(groupingBy(Entry::getKey, mapping(Entry::getValue, toSet())));

    if (permissionReplacements.isEmpty()) {
      return empty();
    }

    var oldCapabilities = capabilityService.findByPermissionNames(permissionReplacements.keySet());
    var oldCapabilitySets = capabilitySetService.findByPermissionNames(permissionReplacements.keySet());

    // Determine which users and roles have assignments of old (replaced) capabilities/capability-sets
    var capabilityRoleAssignments = extractAssignments(oldCapabilities,
      capability -> roleCapabilityRepository.findAllByCapabilityId(capability.getId()), Capability::getPermission,
      RoleCapabilityEntity::getRoleId);

    var capabilitySetRoleAssignments = extractAssignments(oldCapabilitySets,
      capabilitySet -> roleCapabilitySetRepository.findAllByCapabilitySetId(capabilitySet.getId()),
      CapabilitySet::getPermission, RoleCapabilitySetEntity::getRoleId);

    var capabilityUserAssignments = extractAssignments(oldCapabilities,
      capability -> userCapabilityRepository.findAllByCapabilityId(capability.getId()), Capability::getPermission,
      UserCapabilityEntity::getUserId);

    var capabilitySetUserAssignments = extractAssignments(oldCapabilitySets,
      capabilitySet -> userCapabilitySetRepository.findAllByCapabilitySetId(capabilitySet.getId()),
      CapabilitySet::getPermission, UserCapabilitySetEntity::getUserId);

    log.info("Found capability replacements for {} capabilities and capability sets", permissionReplacements.size());
    return Optional.of(
      new CapabilityReplacements(permissionReplacements, capabilityRoleAssignments, capabilityUserAssignments,
        capabilitySetRoleAssignments, capabilitySetUserAssignments));
  }

  protected <S, A> Map<String, Set<UUID>> extractAssignments(List<S> capabilityOrCapabilitySet,
    Function<S, Collection<A>> assignmentsLookup, Function<S, String> sourceToName,
    Function<A, UUID> assignmentToTargetUuid) {

    if (capabilityOrCapabilitySet.isEmpty()) {
      return Map.of();
    }

    return capabilityOrCapabilitySet.stream().map(capability ->
        entry(sourceToName.apply(capability), assignmentsLookup.apply(capability).stream()
          .map(assignmentToTargetUuid)
          .collect(toSet())))
      .collect(toMap(Entry::getKey, Entry::getValue));
  }

  protected void assignReplacementCapabilities(CapabilityReplacements capabilityReplacements) {
    capabilityReplacements.oldPermissionsToNewPermissions().forEach((oldPermissionName, replacements) -> {
      if (isNotEmpty(replacements)) {
        var replacementCapabilities = capabilityService.findByPermissionNames(replacements);
        var replacementCapabilitySets = capabilitySetService.findByPermissionNames(replacements);
        assignReplacementsToRoles(capabilityReplacements.oldRoleCapabByPermission().get(oldPermissionName),
          replacementCapabilities, replacementCapabilitySets);
        assignReplacementsToRoles(capabilityReplacements.oldRoleCapabSetByPermission().get(oldPermissionName),
          replacementCapabilities, replacementCapabilitySets);
        assignReplacementsToUsers(capabilityReplacements.oldUserCapabByPermission().get(oldPermissionName),
          replacementCapabilities, replacementCapabilitySets);
        assignReplacementsToUsers(capabilityReplacements.oldUserCapabSetByPermission().get(oldPermissionName),
          replacementCapabilities, replacementCapabilitySets);
      }
    });
  }

  protected void replaceLoadable(CapabilityReplacements capabilityReplacements) {
    if (MapUtils.isEmpty(capabilityReplacements.oldPermissionsToNewPermissions())) {
      return;
    }

    var oldCapabilities =
      capabilityService.findByPermissionNames(capabilityReplacements.oldPermissionsToNewPermissions().keySet()).stream()
        .collect(toMap(Capability::getPermission, cap -> cap));
    var oldCapabilitySets =
      capabilitySetService.findByPermissionNames(capabilityReplacements.oldPermissionsToNewPermissions().keySet())
        .stream()
        .collect(toMap(CapabilitySet::getPermission, capSet -> capSet));

    for (var oldPermToNewPerms : capabilityReplacements.oldPermissionsToNewPermissions().entrySet()) {
      var oldPermission = oldPermToNewPerms.getKey();
      var newPermissions = oldPermToNewPerms.getValue().stream().toList();
      if (isNotEmpty(newPermissions)) {
        log.info("Processing loadable permissions replacements for old permission: {}", oldPermission);

        var oldCap = oldCapabilities.get(oldPermission);
        var oldCapSet = oldCapabilitySets.get(oldPermission);

        var replacedLoadablePermissionIds = new HashSet<LoadablePermissionKey>();
        if (oldCap != null) {
          replacedLoadablePermissionIds.addAll(replaceLoadablePermissionsForCapability(oldCap, newPermissions));
        }
        if (oldCapSet != null) {
          replacedLoadablePermissionIds.addAll(replaceLoadablePermissionsForCapabilitySet(oldCapSet, newPermissions));
        }
        if (!replacedLoadablePermissionIds.isEmpty()) {
          loadablePermissionRepository.deleteAllById(replacedLoadablePermissionIds);
        }
      }
    }
  }

  protected Set<LoadablePermissionKey> replaceLoadablePermissionsForCapability(Capability oldCap,
    List<String> newPermissions) {
    var loadablePermissions = loadablePermissionRepository.findAllByCapabilityId(oldCap.getId()).toList();
    if (isEmpty(loadablePermissions)) {
      log.debug("No loadable permissions found for capability, capabilityId = {}, permission = {}",
        oldCap.getId(), oldCap.getPermission());
      return emptySet();
    }

    var replacedLoadablePermissionIds = new HashSet<LoadablePermissionKey>();
    for (var loadablePermission : loadablePermissions) {
      for (var permission : newPermissions) {
        capabilityService.findByPermissionName(permission).ifPresent(capability -> {
          var newLoadablePermission = new LoadablePermissionEntity();
          newLoadablePermission.setCapabilityId(capability.getId());
          newLoadablePermission.setPermissionName(capability.getPermission());
          newLoadablePermission.setRoleId(loadablePermission.getRoleId());
          newLoadablePermission.setRole(loadablePermission.getRole());

          log.info("Storing capability replacement {} for loadable permission {} of role {}",
            capability.getPermission(), loadablePermission.getPermissionName(), loadablePermission.getRoleId());

          loadablePermissionRepository.save(newLoadablePermission);
        });
      }
      log.info("Removing replaced loadable permission {} of role {}", loadablePermission.getPermissionName(),
        loadablePermission.getRoleId());
      replacedLoadablePermissionIds.add(loadablePermission.getId());
    }
    return replacedLoadablePermissionIds;
  }

  protected Set<LoadablePermissionKey> replaceLoadablePermissionsForCapabilitySet(CapabilitySet oldCapSet,
    List<String> newPermissions) {
    var loadablePermissions = loadablePermissionRepository.findAllByCapabilitySetId(oldCapSet.getId()).toList();
    if (isEmpty(loadablePermissions)) {
      log.debug("No loadable permissions found for capability set: capabilitySetId = {}, permission = {}",
        oldCapSet.getId(), oldCapSet.getPermission());
      return emptySet();
    }

    var replacedLoadablePermissionIds = new HashSet<LoadablePermissionKey>();
    for (var loadablePermission : loadablePermissions) {
      for (var permission : newPermissions) {
        capabilitySetService.findByPermissionName(permission).ifPresent(capabilitySet -> {
          var newLoadablePermission = new LoadablePermissionEntity();
          newLoadablePermission.setCapabilitySetId(capabilitySet.getId());
          newLoadablePermission.setPermissionName(capabilitySet.getPermission());
          newLoadablePermission.setRoleId(loadablePermission.getRoleId());
          newLoadablePermission.setRole(loadablePermission.getRole());

          log.info("Storing capability set replacement {} for loadable permission {} of role {}",
            capabilitySet.getPermission(), loadablePermission.getPermissionName(), loadablePermission.getRoleId());

          loadablePermissionRepository.save(newLoadablePermission);
        });
      }
      log.info("Removing replaced loadable permission {} of role {}", loadablePermission.getPermissionName(),
        loadablePermission.getRoleId());
      replacedLoadablePermissionIds.add(loadablePermission.getId());
    }
    return replacedLoadablePermissionIds;
  }

  protected void assignReplacementsToRoles(Set<UUID> roleIds, List<Capability> replacementCapabilities,
    List<CapabilitySet> replacementCapabilitySets) {
    if (roleIds != null && !roleIds.isEmpty()) {
      if (!replacementCapabilities.isEmpty()) {
        log.info("Assigning replacement capabilities {} to {} roles",
          replacementCapabilities.stream().map(Capability::getName).collect(Collectors.joining(", ")), roleIds.size());
        roleIds.forEach(doIgnoringExistingAssignments(roleId -> roleCapabilityService.create(roleId,
          replacementCapabilities.stream().map(Capability::getId).toList(), true)));
      }
      if (!replacementCapabilitySets.isEmpty()) {
        log.info("Assigning replacement capability sets {} to {} roles",
          replacementCapabilitySets.stream().map(CapabilitySet::getName).collect(Collectors.joining(", ")),
          roleIds.size());
        roleIds.forEach(doIgnoringExistingAssignments(roleId -> roleCapabilitySetService.create(roleId,
          replacementCapabilitySets.stream().map(CapabilitySet::getId).toList(), true)));
      }
    }
  }

  protected void assignReplacementsToUsers(Set<UUID> userIds, List<Capability> replacementCapabilities,
    List<CapabilitySet> replacementCapabilitySets) {
    if (userIds != null && !userIds.isEmpty()) {
      if (!replacementCapabilities.isEmpty()) {
        log.info("Assigning replacement capabilities {} to {} users",
          replacementCapabilities.stream().map(Capability::getName).collect(Collectors.joining(", ")), userIds.size());
        userIds.forEach(doIgnoringExistingAssignments(userId -> userCapabilityService.create(userId,
          replacementCapabilities.stream().map(Capability::getId).toList())));
      }
      if (!replacementCapabilitySets.isEmpty()) {
        log.info("Assigning replacement capability sets {} to {} users",
          replacementCapabilitySets.stream().map(CapabilitySet::getName).collect(Collectors.joining(", ")),
          userIds.size());
        userIds.forEach(doIgnoringExistingAssignments(userId -> userCapabilitySetService.create(userId,
          replacementCapabilitySets.stream().map(CapabilitySet::getId).toList())));
      }
    }
  }

  protected void unassignReplacedCapabilities(CapabilityReplacements capabilityReplacements) {
    var oldPermissionsToRemove = capabilityReplacements.oldPermissionsToNewPermissions().keySet();

    log.info("Removing old capabilities and capability sets by permission names: {}",
      String.join(", ", oldPermissionsToRemove));
    var oldCapabilitiesToRemove = capabilityService.findByPermissionNames(oldPermissionsToRemove);
    oldCapabilitiesToRemove.stream()
      .map(mapToDeleteCapabilityAppEvent())
      .forEach(applicationEventPublisher::publishEvent);

    var oldCapabilitySetsToRemove = capabilitySetService.findByPermissionNames(oldPermissionsToRemove);
    oldCapabilitySetsToRemove.stream()
      .map(capSet -> capabilitySetMapper.toExtendedCapabilitySet(capSet, emptyList()))
      .map(capSetExt -> deleted(capSetExt).withContext(folioExecutionContext))
      .forEach(applicationEventPublisher::publishEvent);
  }

  protected <T> Consumer<T> doIgnoringExistingAssignments(Consumer<T> action) {
    return v -> {
      try {
        action.accept(v);
      } catch (EntityExistsException existsException) {
        // Ignore case when user/role to capability relation already exists
      }
    };
  }

  private static Stream<Entry<String, String>> mapToReplacesByPermission(Permission perm) {
    return toStream(perm.getReplaces()).map(replacesValue -> entry(replacesValue, perm.getPermissionName()));
  }

  private Function<Capability, DomainEvent<Capability>> mapToDeleteCapabilityAppEvent() {
    return deprecatedCapability -> org.folio.roles.domain.model.event.CapabilityEvent.deleted(deprecatedCapability)
      .withContext(folioExecutionContext);
  }
}
