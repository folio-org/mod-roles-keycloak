package org.folio.roles.service.capability;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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
  private final CapabilitySetByCapabilitiesUpdater capabilitySetByCapabilitiesUpdater;

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

    var capabilitiesByPermissionName = capabilityService
      .findByPermissionNamesIncludeDummy(permissionReplacements.keySet());
    var oldCapabilities = toStream(capabilitiesByPermissionName).filter(c -> FALSE.equals(
      c.getDummyCapability())).toList();
    var oldDummyCapabilities = toStream(capabilitiesByPermissionName).filter(c -> TRUE.equals(
      c.getDummyCapability())).toList();
    var permissionsForDummyCapabilities = toStream(oldDummyCapabilities)
      .map(Capability::getPermission).collect(toSet());

    var permissionReplacementsForCapabilitySet = new HashSet<>(permissionReplacements.keySet());
    permissionReplacementsForCapabilitySet.removeAll(permissionsForDummyCapabilities);
    var oldCapabilitySets = capabilitySetService.findByPermissionNames(permissionReplacementsForCapabilitySet);

    // Determine which users and roles have assignments to old (replaced) capabilities/capability-sets
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

    // Determine which capabilities sets have assignments to old (replaced) dummy capabilities
    var dummyCapabilityCapabilitySetAssignments = extractAssignments(oldDummyCapabilities,
      capability -> capabilitySetService.findAllByCapabilityId(capability.getId()),
      Capability::getPermission, Function.identity());

    log.info("Found capability replacements for {} capabilities and capability sets", permissionReplacements.size());
    return Optional.of(
      new CapabilityReplacements(permissionReplacements, capabilityRoleAssignments,
        capabilityUserAssignments, capabilitySetRoleAssignments,
        capabilitySetUserAssignments, dummyCapabilityCapabilitySetAssignments));
  }

  protected <S, A, U> Map<String, Set<U>> extractAssignments(List<S> capabilityOrCapabilitySet,
    Function<S, Collection<A>> assignmentsLookup, Function<S, String> sourceToName,
    Function<A, U> assignmentToTargetValue) {

    if (capabilityOrCapabilitySet.isEmpty()) {
      return Map.of();
    }

    return capabilityOrCapabilitySet.stream().map(capability ->
        entry(sourceToName.apply(capability), assignmentsLookup.apply(capability).stream()
          .map(assignmentToTargetValue)
          .collect(toSet())))
      .collect(toMap(Entry::getKey, Entry::getValue, CapabilityReplacementsService::mergeSets));
  }

  protected void assignReplacementCapabilities(CapabilityReplacements capabilityReplacements) {
    // dummy capabilities are related only to capability sets, so we should not assign them to roles/users
    capabilityReplacements.getReplacementsExcludeDummy()
      .forEach((oldPermissionName, replacements) -> {
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
    capabilityReplacements.getReplacementsOnlyDummy()
      .forEach((oldPermissionName, replacements) -> {
        if (isNotEmpty(replacements)) {
          var replacementCapabilities = capabilityService.findByPermissionNames(replacements);
          assignReplacementsToCapabilitySet(capabilityReplacements
            .oldCapabSetByDummyCapabilityPermission().get(oldPermissionName), replacementCapabilities);
        }
      });
  }

  protected void replaceLoadable(CapabilityReplacements capabilityReplacements) {
    var oldPermissionsToNewPermissions = capabilityReplacements.getReplacementsExcludeDummy();
    if (MapUtils.isEmpty(oldPermissionsToNewPermissions)) {
      return;
    }

    var oldCapabilities =
      capabilityService.findByPermissionNames(oldPermissionsToNewPermissions.keySet()).stream()
        .collect(toMap(Capability::getPermission, cap -> cap));
    var oldCapabilitySets =
      capabilitySetService.findByPermissionNames(oldPermissionsToNewPermissions.keySet())
        .stream()
        .collect(toMap(CapabilitySet::getPermission, capSet -> capSet));

    for (var oldPermToNewPerms : oldPermissionsToNewPermissions.entrySet()) {
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
          toStream(replacementCapabilities).map(Capability::getName).collect(Collectors.joining(", ")),
          roleIds.size());
        roleIds.forEach(doIgnoringExistingAssignments(roleId -> roleCapabilityService.create(roleId,
          replacementCapabilities.stream().map(Capability::getId).toList(), true)));
      }
      if (!replacementCapabilitySets.isEmpty()) {
        log.info("Assigning replacement capability sets {} to {} roles",
          toStream(replacementCapabilitySets).map(CapabilitySet::getName).collect(Collectors.joining(", ")),
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
          toStream(replacementCapabilities).map(Capability::getName).collect(Collectors.joining(", ")), userIds.size());
        userIds.forEach(doIgnoringExistingAssignments(userId -> userCapabilityService.create(userId,
          replacementCapabilities.stream().map(Capability::getId).toList())));
      }
      if (!replacementCapabilitySets.isEmpty()) {
        log.info("Assigning replacement capability sets {} to {} users",
          toStream(replacementCapabilitySets).map(CapabilitySet::getName).collect(Collectors.joining(", ")),
          userIds.size());
        userIds.forEach(doIgnoringExistingAssignments(userId -> userCapabilitySetService.create(userId,
          replacementCapabilitySets.stream().map(CapabilitySet::getId).toList())));
      }
    }
  }

  protected void assignReplacementsToCapabilitySet(Set<CapabilitySet> capabilitySets,
    List<Capability> replacementCapabilities) {
    if (capabilitySets != null && !capabilitySets.isEmpty()) {
      log.info("Assigning replacement capabilities {} to {} capabilities set",
        toStream(replacementCapabilities).map(Capability::getName).collect(Collectors.joining(", ")),
        capabilitySets.size());
      capabilitySets.forEach(capabilitySet -> capabilitySetByCapabilitiesUpdater
          .update(capabilitySet, replacementCapabilities));
    }
  }

  protected void unassignReplacedCapabilities(CapabilityReplacements capabilityReplacements) {
    var oldPermissionsToRemove = capabilityReplacements.oldPermissionsToNewPermissions().keySet();

    log.info("Removing old capabilities and capability sets by permission names: {}",
      String.join(", ", oldPermissionsToRemove));
    var oldCapabilitiesToRemove = capabilityService.findByPermissionNamesIncludeDummy(oldPermissionsToRemove);
    oldCapabilitiesToRemove.stream()
      .map(mapToDeleteCapabilityAppEvent())
      .forEach(applicationEventPublisher::publishEvent);
    // Should not be a capabilitySet by permission name of dummy capability for unassigning logic
    oldPermissionsToRemove = capabilityReplacements.getReplacementsExcludeDummy().keySet();
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

  private static <T> Set<T> mergeSets(Set<T> existingSet, Set<T> newSet) {
    existingSet.addAll(newSet);
    return existingSet;
  }

  private Function<Capability, DomainEvent<Capability>> mapToDeleteCapabilityAppEvent() {
    return deprecatedCapability -> org.folio.roles.domain.model.event.CapabilityEvent.deleted(deprecatedCapability)
      .withContext(folioExecutionContext);
  }
}
