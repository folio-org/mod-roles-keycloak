package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import jakarta.persistence.EntityExistsException;
import java.util.Collection;
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
import org.folio.common.utils.permission.PermissionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.model.CapabilityReplacements;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.permission.PermissionOverrider;
import org.folio.roles.utils.CapabilityUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CapabilityReplacementsService {

  private final FolioExecutionContext folioExecutionContext;
  private final PermissionOverrider permissionOverrider;
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

  /**
   * Process permission replacements.
   *
   * @param permissionReplacements a map of old to new permissions, e.g. {"old" : ["new1", "new2"]}
   */
  public void processReplacements(CapabilityReplacements permissionReplacements) {
    assignReplacementCapabilities(permissionReplacements);
    unassignReplacedCapabilities(permissionReplacements);
  }

  public Optional<CapabilityReplacements> deduceReplacements(CapabilityEvent newValue) {
    // Create a map of replaced (old) capabilities to list of replacing (new) capabilities,
    // e.g. {"old" : ["new1", "new2"]}
    Map<String, Set<String>> capabilityReplacements;
    if (newValue != null && newValue.getResources() != null) {
      var permissionsWithReplacements =
        newValue.getResources().stream().map(FolioResource::getPermission).filter(Objects::nonNull)
          .filter(p -> p.getReplaces() != null && !p.getReplaces().isEmpty());

      capabilityReplacements = permissionsWithReplacements.flatMap(
          permission -> getPermissionReplacementsAsCapabilities(permission).map(replacesValue -> entry(replacesValue,
            permissionNameToCapabilityName(applyFolioPermissionOverrides(permission.getPermissionName())))))
        .filter(entry -> entry.getValue().isPresent()).map(entry -> entry(entry.getKey(), entry.getValue().get()))
        .collect(groupingBy(Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toSet())));

      if (capabilityReplacements.isEmpty()) {
        return empty();
      }

      var oldCapabilities = capabilityService.findByNames(capabilityReplacements.keySet());
      var oldCapabilitySets = capabilitySetService.findByNames(capabilityReplacements.keySet());

      // Determine which users and roles have assignments of old (replaced) capabilities/capability-sets
      var capabilityRoleAssignments = extractAssignments(oldCapabilities,
        capability -> roleCapabilityRepository.findAllByCapabilityId(capability.getId()), Capability::getName,
        RoleCapabilityEntity::getRoleId);

      var capabilitySetRoleAssignments = extractAssignments(oldCapabilitySets,
        capabilitySet -> roleCapabilitySetRepository.findAllByCapabilitySetId(capabilitySet.getId()),
        CapabilitySet::getName, RoleCapabilitySetEntity::getRoleId);

      var capabilityUserAssignments = extractAssignments(oldCapabilities,
        capability -> userCapabilityRepository.findAllByCapabilityId(capability.getId()), Capability::getName,
        UserCapabilityEntity::getUserId);

      var capabilitySetUserAssignments = extractAssignments(oldCapabilitySets,
        capabilitySet -> userCapabilitySetRepository.findAllByCapabilitySetId(capabilitySet.getId()),
        CapabilitySet::getName, UserCapabilitySetEntity::getUserId);

      return Optional.of(
        new CapabilityReplacements(capabilityReplacements, capabilityRoleAssignments, capabilityUserAssignments,
          capabilitySetRoleAssignments, capabilitySetUserAssignments));
    }
    return empty();
  }

  protected <S, A> Map<String, Set<UUID>> extractAssignments(List<S> capabilityOrCapabilitySet,
    Function<S, Collection<A>> assignmentsLookup, Function<S, String> sourceToName,
    Function<A, UUID> assignmentToTargetUuid) {

    if (capabilityOrCapabilitySet.isEmpty()) {
      return Map.of();
    }

    return capabilityOrCapabilitySet.stream().map(capability -> entry(sourceToName.apply(capability),
        assignmentsLookup.apply(capability).stream().map(assignmentToTargetUuid).collect(Collectors.toSet())))
      .collect(toMap(Entry::getKey, Entry::getValue));
  }

  protected Stream<String> getPermissionReplacementsAsCapabilities(Permission permission) {
    return permission.getReplaces().stream().filter(Objects::nonNull).map(this::applyFolioPermissionOverrides)
      .map(this::permissionNameToCapabilityName).filter(Optional::isPresent).map(Optional::get);
  }

  protected String applyFolioPermissionOverrides(String permissionName) {
    var mappedPermission = permissionOverrider.getPermissionMappings().get(permissionName);
    if (mappedPermission != null) {
      return mappedPermission.getPermissionName();
    }
    return permissionName;
  }

  protected Optional<String> permissionNameToCapabilityName(String permissionName) {
    return Optional.of(permissionName).map(PermissionUtils::extractPermissionData)
      .filter(PermissionUtils::hasRequiredFields).map(CapabilityUtils::getCapabilityName);
  }

  protected void assignReplacementCapabilities(CapabilityReplacements permissionReplacements) {
    permissionReplacements.oldCapabilitiesToNewCapabilities().forEach((oldCapabilityName, replacements) -> {
      if (replacements != null && !replacements.isEmpty()) {
        var replacementCapabilities = capabilityService.findByNames(replacements);
        var replacementCapabilitySets = capabilitySetService.findByNames(replacements);
        assignReplacementsToRoles(permissionReplacements.oldCapabilityRoleAssignments().get(oldCapabilityName),
          replacementCapabilities, replacementCapabilitySets);
        assignReplacementsToRoles(permissionReplacements.oldCapabilitySetRoleAssignments().get(oldCapabilityName),
          replacementCapabilities, replacementCapabilitySets);
        assignReplacementsToUsers(permissionReplacements.oldCapabilityUserAssignments().get(oldCapabilityName),
          replacementCapabilities, replacementCapabilitySets);
        assignReplacementsToUsers(permissionReplacements.oldCapabilitySetUserAssignments().get(oldCapabilityName),
          replacementCapabilities, replacementCapabilitySets);
      }
    });
  }

  protected void assignReplacementsToRoles(Set<UUID> roleIds, List<Capability> replacementCapabilities,
    List<CapabilitySet> replacementCapabilitySets) {
    if (roleIds != null && !roleIds.isEmpty()) {
      if (!replacementCapabilities.isEmpty()) {
        roleIds.forEach(doIgnoringExistingAssignments(roleId -> roleCapabilityService.create(roleId,
          replacementCapabilities.stream().map(Capability::getId).toList(), true)));
      }
      if (!replacementCapabilitySets.isEmpty()) {
        roleIds.forEach(doIgnoringExistingAssignments(roleId -> roleCapabilitySetService.create(roleId,
          replacementCapabilitySets.stream().map(CapabilitySet::getId).toList(), true)));
      }
    }
  }

  protected void assignReplacementsToUsers(Set<UUID> userIds, List<Capability> replacementCapabilities,
    List<CapabilitySet> replacementCapabilitySets) {
    if (userIds != null && !userIds.isEmpty()) {
      if (!replacementCapabilities.isEmpty()) {
        userIds.forEach(doIgnoringExistingAssignments(userId -> userCapabilityService.create(userId,
          replacementCapabilities.stream().map(Capability::getId).toList())));
      }
      if (!replacementCapabilitySets.isEmpty()) {
        userIds.forEach(doIgnoringExistingAssignments(userId -> userCapabilitySetService.create(userId,
          replacementCapabilitySets.stream().map(CapabilitySet::getId).toList())));
      }
    }
  }

  protected void unassignReplacedCapabilities(CapabilityReplacements permissionReplacements) {
    var oldCapabilitiesAndSetsToRemove = permissionReplacements.oldCapabilitiesToNewCapabilities().keySet();

    var oldCapabilitiesToRemove = capabilityService.findByNames(oldCapabilitiesAndSetsToRemove);
    oldCapabilitiesToRemove.stream().map(
      deprecatedCapability -> org.folio.roles.domain.model.event.CapabilityEvent.deleted(deprecatedCapability)
        .withContext(folioExecutionContext)).forEach(applicationEventPublisher::publishEvent);

    var oldCapabilitySetsToRemove = capabilitySetService.findByNames(oldCapabilitiesAndSetsToRemove);
    oldCapabilitySetsToRemove.stream().map(capSet -> capabilitySetMapper.toExtendedCapabilitySet(capSet, emptyList()))
      .map(capSetExt -> CapabilitySetEvent.deleted(capSetExt).withContext(folioExecutionContext))
      .forEach(applicationEventPublisher::publishEvent);
  }

  protected <T> Consumer<T> doIgnoringExistingAssignments(Consumer<T> action) {
    return v -> {
      try {
        action.accept(v);
      } catch (EntityExistsException existsException) {
        // Ignore case when relation already exists
      }
    };
  }
}
