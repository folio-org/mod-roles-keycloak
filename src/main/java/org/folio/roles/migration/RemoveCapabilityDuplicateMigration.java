package org.folio.roles.migration;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.common.utils.CollectionUtils.mapItems;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import liquibase.database.Database;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.domain.entity.CapabilityEntity;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Universal Liquibase custom migration to cleanup duplicate capabilities.
 *
 * <p>This migration removes old capability and capability_set after migrating all references to new ones.</p>
 *
 * <p>Usage in Liquibase XML:</p>
 * <pre>
 * &lt;customChange class="org.folio.roles.migration.RemoveCapabilityDuplicateMigration"&gt;
 *   &lt;param name="oldCapabilityName" value="old-name"/&gt;
 *   &lt;param name="newCapabilityName" value="new-name"/&gt;
 * &lt;/customChange&gt;
 * </pre>
 *
 * <p>Migration steps:</p>
 * <ol>
 *   <li>Find OLD and NEW capability/capability_set by name</li>
 *   <li>Migrate role_capability references (OLD → NEW)</li>
 *   <li>Migrate user_capability references (OLD → NEW)</li>
 *   <li>Migrate role_capability_set references (OLD → NEW)</li>
 *   <li>Migrate user_capability_set references (OLD → NEW)</li>
 *   <li>Synchronize Keycloak permissions</li>
 *   <li>Delete OLD capability/capability_set through EventHandler</li>
 * </ol>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Idempotent - can be run multiple times</li>
 *   <li>Transactional - all or nothing</li>
 *   <li>Handles duplicates - if role/user already has NEW, just deletes OLD</li>
 *   <li>Keycloak sync - updates permissions in Keycloak</li>
 * </ul>
 */
@Log4j2
@Setter
public class RemoveCapabilityDuplicateMigration extends AbstractCustomTaskChangeMigration {

  private String oldCapabilityName;
  private String newCapabilityName;

  @Override
  @Transactional
  public void execute(Database database) {
    log.info("Starting capability duplicate cleanup migration: old='{}', new='{}'",
      oldCapabilityName, newCapabilityName);

    validateParameters();

    var capabilityRepository = springApplicationContext.getBean(CapabilityRepository.class);
    var capabilitySetRepository = springApplicationContext.getBean(CapabilitySetRepository.class);

    var oldCapabilityOpt = capabilityRepository.findByName(oldCapabilityName);
    var newCapabilityOpt = capabilityRepository.findByName(newCapabilityName);
    var oldCapabilitySetOpt = capabilitySetRepository.findByName(oldCapabilityName);
    var newCapabilitySetOpt = capabilitySetRepository.findByName(newCapabilityName);

    if (shouldSkipMigration(oldCapabilityOpt, oldCapabilitySetOpt, newCapabilityOpt, newCapabilitySetOpt)) {
      return;
    }

    if (oldCapabilityOpt.isPresent()) {
      migrateCapability(oldCapabilityOpt.get(), newCapabilityOpt.get());
    }

    if (oldCapabilitySetOpt.isPresent()) {
      migrateCapabilitySet(oldCapabilitySetOpt.get(), newCapabilitySetOpt.get());
    }

    log.info("Capability duplicate cleanup migration completed successfully");
  }

  private boolean shouldSkipMigration(Optional<CapabilityEntity> oldCapabilityOpt,
    Optional<CapabilitySetEntity> oldCapabilitySetOpt, Optional<CapabilityEntity> newCapabilityOpt,
    Optional<CapabilitySetEntity> newCapabilitySetOpt) {

    if (oldCapabilityOpt.isEmpty() && oldCapabilitySetOpt.isEmpty()) {
      log.info("OLD capability/capability_set not found - migration already completed, skipping");
      return true;
    }

    if (newCapabilityOpt.isEmpty() || newCapabilitySetOpt.isEmpty()) {
      log.warn("NEW capability/capability_set not found: capability={}, capabilitySet={} - skipping migration",
        newCapabilityOpt.isPresent(), newCapabilitySetOpt.isPresent());
      return true;
    }

    return false;
  }

  private void migrateCapability(CapabilityEntity oldCap, CapabilityEntity newCap) {
    log.info("Migrating capability: {} -> {}", oldCap.getId(), newCap.getId());

    var roleCapabilityRepository = springApplicationContext.getBean(RoleCapabilityRepository.class);
    var userCapabilityRepository = springApplicationContext.getBean(UserCapabilityRepository.class);

    migrateRoleCapabilities(oldCap.getId(), newCap.getId(), roleCapabilityRepository);
    migrateUserCapabilities(oldCap.getId(), newCap.getId(), userCapabilityRepository);

    var capabilityMapper = springApplicationContext.getBean(CapabilityEntityMapper.class);
    var capabilityDto = capabilityMapper.convert(oldCap);
    var event = CapabilityEvent.deleted(capabilityDto);
    springApplicationContext.publishEvent(event);
    log.info("Published delete event for capability: {}", oldCap.getName());
  }

  private void migrateCapabilitySet(CapabilitySetEntity oldCapSet, CapabilitySetEntity newCapSet) {
    log.info("Migrating capability_set: {} -> {}", oldCapSet.getId(), newCapSet.getId());

    var roleCapabilitySetRepository = springApplicationContext.getBean(RoleCapabilitySetRepository.class);
    var userCapabilitySetRepository = springApplicationContext.getBean(UserCapabilitySetRepository.class);

    migrateRoleCapabilitySets(oldCapSet.getId(), newCapSet.getId(), roleCapabilitySetRepository);
    migrateUserCapabilitySets(oldCapSet.getId(), newCapSet.getId(), userCapabilitySetRepository);

    var capabilityService = springApplicationContext.getBean(CapabilityService.class);
    var capabilitySetEntityMapper = springApplicationContext.getBean(CapabilitySetEntityMapper.class);
    var capabilitySetMapper = springApplicationContext.getBean(CapabilitySetMapper.class);

    var capabilitySetDto = capabilitySetEntityMapper.convert(oldCapSet);
    var capabilities = capabilityService.findByCapabilitySetIdsIncludeDummy(Set.of(oldCapSet.getId()));
    var extendedCapabilitySet = capabilitySetMapper.toExtendedCapabilitySet(capabilitySetDto, capabilities);
    var event = CapabilitySetEvent.deleted(extendedCapabilitySet);
    springApplicationContext.publishEvent(event);
    log.info("Published delete event for capability_set: {}", oldCapSet.getName());
  }

  private void validateParameters() {
    if (isBlank(oldCapabilityName)) {
      throw new IllegalArgumentException("oldCapabilityName parameter is required");
    }
    if (isBlank(newCapabilityName)) {
      throw new IllegalArgumentException("newCapabilityName parameter is required");
    }
  }

  private void migrateRoleCapabilities(UUID oldId, UUID newId, RoleCapabilityRepository repository) {
    log.debug("Migrating role_capability: {} -> {}", oldId, newId);
    var oldRelations = repository.findAllByCapabilityId(oldId);

    int migrated = 0;
    int deleted = 0;

    for (var relation : oldRelations) {
      boolean newExists = repository.existsByRoleIdAndCapabilityId(relation.getRoleId(), newId);

      if (newExists) {
        // Role already has NEW capability - just delete OLD
        repository.delete(relation);
        deleted++;
      } else {
        // Migrate OLD -> NEW
        relation.setCapabilityId(newId);
        repository.save(relation);
        migrated++;
      }
    }

    log.info("role_capability migration completed: migrated={}, deleted={}", migrated, deleted);
  }

  private void migrateUserCapabilities(UUID oldId, UUID newId, UserCapabilityRepository repository) {
    log.debug("Migrating user_capability: {} -> {}", oldId, newId);
    var oldRelations = repository.findAllByCapabilityId(oldId);

    int migrated = 0;
    int deleted = 0;

    for (var relation : oldRelations) {
      boolean newExists = repository.existsByUserIdAndCapabilityId(relation.getUserId(), newId);

      if (newExists) {
        repository.delete(relation);
        deleted++;
      } else {
        relation.setCapabilityId(newId);
        repository.save(relation);
        migrated++;
      }
    }

    log.info("user_capability migration completed: migrated={}, deleted={}", migrated, deleted);
  }

  private void migrateRoleCapabilitySets(UUID oldId, UUID newId, RoleCapabilitySetRepository repository) {
    log.debug("Migrating role_capability_set: {} -> {}", oldId, newId);
    var oldRelations = repository.findAllByCapabilitySetId(oldId);

    int migrated = 0;
    int deleted = 0;

    for (var relation : oldRelations) {
      boolean newExists = repository.existsByRoleIdAndCapabilitySetId(relation.getRoleId(), newId);

      if (newExists) {
        repository.delete(relation);
        deleted++;
      } else {
        relation.setCapabilitySetId(newId);
        repository.save(relation);
        migrated++;
      }
    }

    log.info("role_capability_set migration completed: migrated={}, deleted={}", migrated, deleted);
  }

  private void migrateUserCapabilitySets(UUID oldId, UUID newId, UserCapabilitySetRepository repository) {
    log.debug("Migrating user_capability_set: {} -> {}", oldId, newId);
    var oldRelations = repository.findAllByCapabilitySetId(oldId);

    int migrated = 0;
    int deleted = 0;

    for (var relation : oldRelations) {
      boolean newExists = repository.existsByUserIdAndCapabilitySetId(relation.getUserId(), newId);

      if (newExists) {
        repository.delete(relation);
        deleted++;
      } else {
        relation.setCapabilitySetId(newId);
        repository.save(relation);
        migrated++;
      }
    }

    log.info("user_capability_set migration completed: migrated={}, deleted={}", migrated, deleted);
  }

  private List<Endpoint> toEndpointDtos(CapabilityEntity entity) {
    return mapItems(entity.getEndpoints(), ep ->
      new Endpoint()
        .path(ep.getPath())
        .method(HttpMethod.valueOf(ep.getMethod().name())));
  }
}
