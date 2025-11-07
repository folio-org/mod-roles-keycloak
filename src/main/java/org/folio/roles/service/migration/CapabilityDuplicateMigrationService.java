package org.folio.roles.service.migration;

import static org.apache.commons.lang3.StringUtils.isBlank;

import jakarta.persistence.EntityExistsException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.service.capability.UserCapabilityService;
import org.folio.roles.service.capability.UserCapabilitySetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring-managed migration service that removes duplicate capabilities and capability sets by
 * orchestrating existing domain services instead of raw SQL.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilityDuplicateMigrationService {

  private final CapabilityService capabilityService;
  private final CapabilitySetService capabilitySetService;
  private final RoleCapabilityService roleCapabilityService;
  private final UserCapabilityService userCapabilityService;
  private final RoleCapabilitySetService roleCapabilitySetService;
  private final UserCapabilitySetService userCapabilitySetService;
  private final RoleCapabilityRepository roleCapabilityRepository;
  private final UserCapabilityRepository userCapabilityRepository;
  private final RoleCapabilitySetRepository roleCapabilitySetRepository;
  private final UserCapabilitySetRepository userCapabilitySetRepository;
  private final LoadablePermissionRepository loadablePermissionRepository;

  /**
   * Migrates all references from {@code oldCapabilityName} to {@code newCapabilityName}.
   *
   * @param oldCapabilityName name of the deprecated capability/capability-set
   * @param newCapabilityName name of the replacement capability/capability-set
   */
  @Transactional
  public void migrate(String oldCapabilityName, String newCapabilityName) {
    if (isBlank(oldCapabilityName) || isBlank(newCapabilityName)) {
      throw new IllegalArgumentException("Capability names must not be blank");
    }

    log.info("Starting capability duplicate migration: old='{}', new='{}'", oldCapabilityName, newCapabilityName);

    var oldCapability = capabilityService.findByName(oldCapabilityName);
    var newCapability = capabilityService.findByName(newCapabilityName);
    var oldCapabilitySet = capabilitySetService.findByName(oldCapabilityName);
    var newCapabilitySet = capabilitySetService.findByName(newCapabilityName);

    if (oldCapability.isEmpty() && oldCapabilitySet.isEmpty()) {
      log.info("Neither capability nor capability set '{}' found. Assuming migration already applied.",
        oldCapabilityName);
      return;
    }

    migrateCapabilityIfPresent(oldCapability, newCapability, oldCapabilityName, newCapabilityName);
    migrateCapabilitySetIfPresent(oldCapabilitySet, newCapabilitySet, oldCapabilityName, newCapabilityName);

    log.info("Capability duplicate migration completed: old='{}', new='{}'", oldCapabilityName, newCapabilityName);
  }

  private void migrateCapabilityIfPresent(Optional<Capability> oldCapability, Optional<Capability> newCapability,
    String oldCapabilityName, String newCapabilityName) {
    oldCapability.ifPresent(capability -> {
      var replacement = newCapability.orElse(null);
      if (replacement == null) {
        log.warn("OLD capability '{}' exists but NEW capability '{}' not found. "
          + "Skipping capability migration.", oldCapabilityName, newCapabilityName);
        return;
      }
      migrateCapability(capability, replacement);
    });
  }

  private void migrateCapabilitySetIfPresent(Optional<CapabilitySet> oldCapabilitySet,
    Optional<CapabilitySet> newCapabilitySet, String oldCapabilityName, String newCapabilityName) {
    oldCapabilitySet.ifPresent(capabilitySet -> {
      var replacement = newCapabilitySet.orElse(null);
      if (replacement == null) {
        log.warn("OLD capability set '{}' exists but NEW capability set '{}' not found. "
          + "Skipping capability set migration.", oldCapabilityName, newCapabilityName);
        return;
      }
      migrateCapabilitySet(capabilitySet, replacement);
    });
  }

  private void migrateCapability(Capability oldCapability, Capability newCapability) {
    var oldId = oldCapability.getId();
    var newId = newCapability.getId();

    log.info("Migrating capability references: {} -> {}", oldId, newId);

    migrateRoleCapabilities(oldId, newId);
    migrateUserCapabilities(oldId, newId);
    migrateLoadablePermissionsForCapability(oldId, newId);

    capabilitySetService.deleteAllLinksToCapability(oldId);
    capabilityService.deleteById(oldId);
  }

  private void migrateCapabilitySet(CapabilitySet oldCapabilitySet, CapabilitySet newCapabilitySet) {
    var oldId = oldCapabilitySet.getId();
    var newId = newCapabilitySet.getId();

    log.info("Migrating capability set references: {} -> {}", oldId, newId);

    migrateRoleCapabilitySets(oldId, newId);
    migrateUserCapabilitySets(oldId, newId);
    migrateLoadablePermissionsForCapabilitySet(oldId, newId);

    capabilitySetService.deleteById(oldId);
  }

  private void migrateRoleCapabilities(UUID oldCapabilityId, UUID newCapabilityId) {
    var assignments = roleCapabilityRepository.findAllByCapabilityId(oldCapabilityId);
    for (RoleCapabilityEntity assignment : assignments) {
      var roleId = assignment.getRoleId();
      roleCapabilityService.create(roleId, List.of(newCapabilityId), true);
      roleCapabilityService.delete(roleId, oldCapabilityId);
    }
  }

  private void migrateUserCapabilities(UUID oldCapabilityId, UUID newCapabilityId) {
    var assignments = userCapabilityRepository.findAllByCapabilityId(oldCapabilityId);
    for (UserCapabilityEntity assignment : assignments) {
      var userId = assignment.getUserId();
      if (!userCapabilityRepository.existsByUserIdAndCapabilityId(userId, newCapabilityId)) {
        try {
          userCapabilityService.create(userId, List.of(newCapabilityId));
        } catch (EntityExistsException existsException) {
          log.debug("Capability {} already assigned to user {}", newCapabilityId, userId, existsException);
        }
      }
      userCapabilityService.delete(userId, oldCapabilityId);
    }
  }

  private void migrateRoleCapabilitySets(UUID oldCapabilitySetId, UUID newCapabilitySetId) {
    var assignments = roleCapabilitySetRepository.findAllByCapabilitySetId(oldCapabilitySetId);
    for (RoleCapabilitySetEntity assignment : assignments) {
      var roleId = assignment.getRoleId();
      roleCapabilitySetService.create(roleId, List.of(newCapabilitySetId), true);
      roleCapabilitySetService.delete(roleId, oldCapabilitySetId);
    }
  }

  private void migrateUserCapabilitySets(UUID oldCapabilitySetId, UUID newCapabilitySetId) {
    var assignments = userCapabilitySetRepository.findAllByCapabilitySetId(oldCapabilitySetId);
    for (UserCapabilitySetEntity assignment : assignments) {
      var userId = assignment.getUserId();
      if (!userCapabilitySetRepository.existsByUserIdAndCapabilitySetId(userId, newCapabilitySetId)) {
        try {
          userCapabilitySetService.create(userId, List.of(newCapabilitySetId));
        } catch (EntityExistsException existsException) {
          log.debug("Capability set {} already assigned to user {}", newCapabilitySetId, userId, existsException);
        }
      }
      userCapabilitySetService.delete(userId, oldCapabilitySetId);
    }
  }

  private void migrateLoadablePermissionsForCapability(UUID oldCapabilityId, UUID newCapabilityId) {
    try (var stream = loadablePermissionRepository.findAllByCapabilityId(oldCapabilityId)) {
      var permissions = stream
        .peek(permission -> permission.setCapabilityId(newCapabilityId))
        .collect(Collectors.toList());
      saveLoadablePermissions(permissions);
    }
  }

  private void migrateLoadablePermissionsForCapabilitySet(UUID oldCapabilitySetId, UUID newCapabilitySetId) {
    try (var stream = loadablePermissionRepository.findAllByCapabilitySetId(oldCapabilitySetId)) {
      var permissions = stream
        .peek(permission -> permission.setCapabilitySetId(newCapabilitySetId))
        .collect(Collectors.toList());
      saveLoadablePermissions(permissions);
    }
  }

  private void saveLoadablePermissions(List<LoadablePermissionEntity> permissions) {
    if (permissions.isEmpty()) {
      return;
    }
    loadablePermissionRepository.saveAll(permissions);
  }
}
