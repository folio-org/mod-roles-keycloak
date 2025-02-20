package org.folio.roles.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySetEntity;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilityEntity;
import static org.folio.roles.support.RoleUtils.roleEntity;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetEntity;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilityEntity;

import java.util.List;
import java.util.UUID;
import org.folio.roles.base.BaseRepositoryTest;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.domain.entity.CapabilityEndpointEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CapabilityEndpointRepositoryIT extends BaseRepositoryTest {

  @Autowired
  private CapabilityEndpointRepository capabilityEndpointRepository;

  private CapabilityEndpointEntity getCapabilityEndpointEntity(UUID capabilityEntityId) {
    var capabilityEndpointEntity = new CapabilityEndpointEntity();
    capabilityEndpointEntity.setCapabilityId(capabilityEntityId);
    capabilityEndpointEntity.setPath("path");
    capabilityEndpointEntity.setMethod(HttpMethod.GET);
    return capabilityEndpointEntity;
  }

  @Test
  void getByRoleId_positive_excludeDummyAndByCapabilitiesIds() {
    var capabilityEntity = capabilityEntity(null);
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    entityManager.persistAndFlush(roleCapabilityEntity(roleId, capabilityEntity.getId()));
    entityManager.persistAndFlush(getCapabilityEndpointEntity(capabilityEntity.getId()));

    var capabilitiesEndpoints = capabilityEndpointRepository.getByRoleId(roleId, null);
    assertThat(capabilitiesEndpoints).hasSize(1);

    capabilityEntity.setDummyCapability(true);
    entityManager.flush();
    capabilitiesEndpoints = capabilityEndpointRepository.getByRoleId(roleId, null);
    assertThat(capabilitiesEndpoints).isEmpty();
  }

  @Test
  void getByRoleId_positive_excludeDummyAndByCapabilitiesIdsCapabilitiesSetIds() {
    var capabilityEntity = capabilityEntity(null);
    var roleId = UUID.randomUUID();
    var roleEntity = roleEntity();
    roleEntity.setId(roleId);
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(roleEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(roleCapabilitySetEntity(roleId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(getCapabilityEndpointEntity(capabilityEntity.getId()));

    var capabilitiesEndpoints = capabilityEndpointRepository.getByRoleId(roleId, null, null);
    assertThat(capabilitiesEndpoints).hasSize(1);

    capabilityEntity.setDummyCapability(true);
    entityManager.flush();
    capabilitiesEndpoints = capabilityEndpointRepository.getByRoleId(roleId, null, null);
    assertThat(capabilitiesEndpoints).isEmpty();

    capabilityEntity.setDummyCapability(false);
    capabilitySetEntity.setDummyCapability(true);
    entityManager.flush();
    capabilitiesEndpoints = capabilityEndpointRepository.getByRoleId(roleId, null, null);
    assertThat(capabilitiesEndpoints).isEmpty();
  }

  @Test
  void getByUserId_excludeDummyAndByCapabilitiesIds() {
    var capabilityEntity = capabilityEntity(null);
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    entityManager.persistAndFlush(userCapabilityEntity(userId, capabilityEntity.getId()));
    entityManager.persistAndFlush(getCapabilityEndpointEntity(capabilityEntity.getId()));

    var capabilitiesEndpoints = capabilityEndpointRepository.getByUserId(userId, null);
    assertThat(capabilitiesEndpoints).hasSize(1);

    capabilityEntity.setDummyCapability(true);
    entityManager.flush();
    capabilitiesEndpoints = capabilityEndpointRepository.getByUserId(userId, null);
    assertThat(capabilitiesEndpoints).isEmpty();
  }

  @Test
  void getByUserId_positive_excludeDummyAndByCapabilitiesIdsCapabilitiesSetIds() {
    var capabilityEntity = capabilityEntity(null);
    var userId = UUID.randomUUID();
    entityManager.persistAndFlush(capabilityEntity);
    var capabilitySetEntity = capabilitySetEntity(null, List.of(capabilityEntity.getId()));
    entityManager.persistAndFlush(capabilitySetEntity);
    entityManager.persistAndFlush(userCapabilitySetEntity(userId, capabilitySetEntity.getId()));
    entityManager.persistAndFlush(getCapabilityEndpointEntity(capabilityEntity.getId()));

    var capabilitiesEndpoints = capabilityEndpointRepository.getByUserId(userId, null, null);
    assertThat(capabilitiesEndpoints).hasSize(1);

    capabilityEntity.setDummyCapability(true);
    entityManager.flush();
    capabilitiesEndpoints = capabilityEndpointRepository.getByUserId(userId, null, null);
    assertThat(capabilitiesEndpoints).isEmpty();

    capabilityEntity.setDummyCapability(false);
    capabilitySetEntity.setDummyCapability(true);
    entityManager.flush();
    capabilitiesEndpoints = capabilityEndpointRepository.getByUserId(userId, null, null);
    assertThat(capabilitiesEndpoints).isEmpty();
  }
}
