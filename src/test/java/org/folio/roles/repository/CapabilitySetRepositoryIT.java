package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.RoleEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.roles.repository.projection.UserCapabilitySetNameProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CapabilitySetRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private CapabilitySetRepository repository;

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = capabilitySetEntity();
    entity.setId(null);
    var now = OffsetDateTime.now();

    var saved = repository.save(entity);

    var stored = entityManager.find(CapabilitySetEntity.class, saved.getId());
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findByCapabilityName_positive() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);

    var actualCapabilitySetEntities = repository.findByCapabilityName(capabilityEntity.getName());
    assertThat(actualCapabilitySetEntities).containsOnly(capabilitySetEntity);
  }

  @Test
  void addCapabilityById_positive() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity = capabilitySetEntity();
    capabilitySetEntity.setId(null);
    capabilitySetEntity.setCapabilities(null);
    capabilitySetEntity = entityManager.persistAndFlush(capabilitySetEntity);

    repository.addCapabilityById(capabilitySetEntity.getId(), capabilityEntity.getId());

    var actualCapabilitySetEntities = repository.findByCapabilityName(capabilityEntity.getName());
    assertThat(actualCapabilitySetEntities).containsOnly(capabilitySetEntity);
  }

  @Test
  void findAllByCapabilityId_positive() {
    var capabilityEntity = capabilityEntity(null);
    capabilityEntity = entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity1 = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    capabilitySetEntity1.setName("capabilitySetEntity1");
    var capabilitySetEntity2 = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    capabilitySetEntity2.setName("capabilitySetEntity2");
    entityManager.persistAndFlush(capabilitySetEntity1);
    entityManager.persistAndFlush(capabilitySetEntity2);

    var actualCapabilitySetEntities = repository.findAllByCapabilityId(capabilityEntity.getId());
    assertThat(actualCapabilitySetEntities).containsExactlyInAnyOrder(capabilitySetEntity1, capabilitySetEntity2);
  }

  @Test
  void findEffectiveCapabilitySetNames_positive_directAndRoleAssignmentsAreDeduplicated() {
    final var userId = UUID.randomUUID();
    final var roleId = UUID.randomUUID();
    var capability = capabilityEntity(null);
    capability = entityManager.persistAndFlush(capability);
    var directAndInheritedSet = capabilitySetEntity(UUID.randomUUID());
    directAndInheritedSet.setId(null);
    directAndInheritedSet.setCapabilities(List.of(capability.getId()));
    directAndInheritedSet.setName("direct-and-inherited");
    var inheritedSet = capabilitySetEntity(UUID.randomUUID());
    inheritedSet.setId(null);
    inheritedSet.setCapabilities(List.of(capability.getId()));
    inheritedSet.setName("inherited-only");
    directAndInheritedSet = entityManager.persistAndFlush(directAndInheritedSet);
    inheritedSet = entityManager.persistAndFlush(inheritedSet);

    var role = new RoleEntity();
    role.setId(roleId);
    role.setName("query-role");
    role.setDescription("query role");
    entityManager.persistAndFlush(role);

    entityManager.persistAndFlush(UserCapabilitySetEntity.of(userId, directAndInheritedSet.getId()));
    var userRole = new UserRoleEntity();
    userRole.setUserId(userId);
    userRole.setRoleId(roleId);
    entityManager.persistAndFlush(userRole);
    entityManager.persistAndFlush(RoleCapabilitySetEntity.of(roleId, directAndInheritedSet.getId()));
    entityManager.persistAndFlush(RoleCapabilitySetEntity.of(roleId, inheritedSet.getId()));

    var actual = repository.findEffectiveCapabilitySetNames(new UUID[] {userId});

    assertThat(actual)
      .extracting(UserCapabilitySetNameProjection::getUserId, UserCapabilitySetNameProjection::getCapabilitySetName)
      .containsExactlyInAnyOrder(
        tuple(userId, "direct-and-inherited"),
        tuple(userId, "inherited-only"));
  }

  @Test
  void findEffectiveCapabilitySetNames_positive_filtersByExactName() {
    final var userId = UUID.randomUUID();
    var capability = capabilityEntity(null);
    capability = entityManager.persistAndFlush(capability);
    var capabilitySet = capabilitySetEntity(UUID.randomUUID());
    capabilitySet.setId(null);
    capabilitySet.setCapabilities(List.of(capability.getId()));
    capabilitySet.setName("exact-name");
    capabilitySet = entityManager.persistAndFlush(capabilitySet);
    entityManager.persistAndFlush(UserCapabilitySetEntity.of(userId, capabilitySet.getId()));

    var actual = repository.findEffectiveCapabilitySetNames(new UUID[] {userId}, new String[] {"exact-name"});

    assertThat(actual).hasSize(1);
    assertThat(actual.getFirst().getCapabilitySetName()).isEqualTo("exact-name");
  }

  @Test
  void findEffectiveCapabilitySetNames_positive_batchLargerThanBindParameterLimit() {
    final var userId = UUID.randomUUID();
    var capability = entityManager.persistAndFlush(capabilityEntity(null));
    var capabilitySet = capabilitySetEntity(UUID.randomUUID());
    capabilitySet.setId(null);
    capabilitySet.setCapabilities(List.of(capability.getId()));
    capabilitySet.setName("large-batch");
    capabilitySet = entityManager.persistAndFlush(capabilitySet);
    entityManager.persistAndFlush(UserCapabilitySetEntity.of(userId, capabilitySet.getId()));

    var userIds = new UUID[40_000];
    userIds[0] = userId;
    for (var index = 1; index < userIds.length; index++) {
      userIds[index] = UUID.randomUUID();
    }

    var actual = repository.findEffectiveCapabilitySetNames(userIds);

    assertThat(actual)
      .extracting(UserCapabilitySetNameProjection::getUserId, UserCapabilitySetNameProjection::getCapabilitySetName)
      .containsExactly(tuple(userId, "large-batch"));
  }
}
