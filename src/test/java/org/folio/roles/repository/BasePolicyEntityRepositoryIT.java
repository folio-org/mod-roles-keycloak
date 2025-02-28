package org.folio.roles.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilityEntity;
import static org.folio.roles.support.RoleUtils.roleEntity;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetEntity;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilityEntity;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.domain.entity.Auditable;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyRoleEntity;
import org.folio.roles.domain.entity.UserPolicyEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BasePolicyEntityRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private PolicyEntityRepository policyEntityRepository;

  private RolePolicyEntity getRolePolicyRoleEntity(UUID roleId) {
    var rolePoliceRoleEntity = new RolePolicyRoleEntity();
    rolePoliceRoleEntity.setId(roleId);
    var rolePolicyEntity = new RolePolicyEntity();
    rolePolicyEntity.setRoles(List.of(rolePoliceRoleEntity));
    rolePolicyEntity.setName("rolePolicyName");
    rolePolicyEntity.setSource(SourceType.USER);
    return rolePolicyEntity;
  }

  private UserPolicyEntity getUserPolicyEntity(UUID userId) {
    var userPolicyEntity = new UserPolicyEntity();
    userPolicyEntity.setName("userPolicyName");
    userPolicyEntity.setSource(SourceType.USER);
    userPolicyEntity.setUsers(List.of(userId));
    return userPolicyEntity;
  }

  @BeforeEach
  void returnTestUserIdFromFolioExecutionContext() {
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
  }

  @Test
  void create_positive_updatedAndCreatedFieldsNotNull() {
    var entity = Instancio.of(BasePolicyEntity.class)
      .ignore(field(Auditable::getCreatedDate))
      .ignore(field(Auditable::getUpdatedDate))
      .create();
    entity.setId(null);
    var now = OffsetDateTime.now();

    var saved = policyEntityRepository.save(entity);

    var stored = entityManager.find(BasePolicyEntity.class, saved.getId());
    assertThat(stored.getCreatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getCreatedByUserId()).isEqualTo(USER_ID);
    assertThat(stored.getUpdatedDate()).isCloseTo(now, within(1, MINUTES));
    assertThat(stored.getUpdatedByUserId()).isEqualTo(USER_ID);
  }

  @Test
  void findRolePoliciesByCapabilitySetId_positive_excludeDummy() {
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(roleEntity);
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(getRolePolicyRoleEntity(roleId));

    var rolePolicies = policyEntityRepository.findRolePoliciesByCapabilitySetId(capabilitySetEntity.getId());
    assertThat(rolePolicies).hasSize(1);
  }

  @Test
  void findUserPoliciesByCapabilitySetId_positive_excludeDummy() {
    var userId = UUID.randomUUID();
    var capabilitySetEntity = capabilitySetEntity(null, emptyList());
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(userCapabilitySetEntity(userId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(getUserPolicyEntity(userId));

    var userPolicies = policyEntityRepository.findUserPoliciesByCapabilitySetId(capabilitySetEntity.getId());
    assertThat(userPolicies).hasSize(1);
  }

  @Test
  void findRolePoliciesByCapabilityId_positive_excludeDummy() {
    var capabilityForRoleEntity = capabilityEntity(null);
    capabilityForRoleEntity.setName("capabilityForRoleName");
    var capabilityForCapabilitySetEntity = capabilityEntity(null);
    capabilityForCapabilitySetEntity.setName("capabilityForCapabilitySetName");
    entityManager.persistAndFlush(capabilityForRoleEntity);
    entityManager.persistAndFlush(capabilityForCapabilitySetEntity);
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(roleEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityForCapabilitySetEntity.getId()));
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityForRoleEntity.getId()));
    entityManager.persistAndFlush(getRolePolicyRoleEntity(roleId));

    var userPolicies = policyEntityRepository.findRolePoliciesByCapabilityId(capabilityForRoleEntity.getId());
    assertThat(userPolicies).hasSize(1);
    userPolicies = policyEntityRepository.findRolePoliciesByCapabilityId(capabilityForCapabilitySetEntity.getId());
    assertThat(userPolicies).hasSize(1);

    capabilityForRoleEntity.setDummyCapability(true);
    entityManager.flush();
    userPolicies = policyEntityRepository.findRolePoliciesByCapabilityId(capabilityForRoleEntity.getId());
    assertThat(userPolicies).isEmpty();

    capabilityForCapabilitySetEntity.setDummyCapability(true);
    entityManager.flush();
    userPolicies = policyEntityRepository.findRolePoliciesByCapabilityId(capabilityForCapabilitySetEntity.getId());
    assertThat(userPolicies).isEmpty();
  }

  @Test
  void findUserPoliciesByCapabilityId_positive_excludeDummy() {
    var capabilityForUserEntity = capabilityEntity(null);
    capabilityForUserEntity.setName("capabilityForUserName");
    var capabilityForCapabilitySetEntity = capabilityEntity(null);
    capabilityForCapabilitySetEntity.setName("capabilityForCapabilitySetName");
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityForUserEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityForUserEntity.getId()));
    entityManager.persistAndFlush(capabilityForCapabilitySetEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityForCapabilitySetEntity.getId()));
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(userCapabilitySetEntity(userId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(getUserPolicyEntity(userId));

    var userPolicies = policyEntityRepository.findUserPoliciesByCapabilityId(capabilityForUserEntity.getId());
    assertThat(userPolicies).hasSize(1);
    userPolicies = policyEntityRepository.findUserPoliciesByCapabilityId(capabilityForCapabilitySetEntity.getId());
    assertThat(userPolicies).hasSize(1);

    capabilityForUserEntity.setDummyCapability(true);
    entityManager.flush();
    userPolicies = policyEntityRepository.findUserPoliciesByCapabilityId(capabilityForUserEntity.getId());
    assertThat(userPolicies).isEmpty();

    capabilityForCapabilitySetEntity.setDummyCapability(true);
    entityManager.flush();
    userPolicies = policyEntityRepository.findUserPoliciesByCapabilityId(capabilityForCapabilitySetEntity.getId());
    assertThat(userPolicies).isEmpty();
  }
}
