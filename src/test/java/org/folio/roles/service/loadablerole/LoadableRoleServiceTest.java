package org.folio.roles.service.loadablerole;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.LoadableRoleUtils.loadableRole;
import static org.folio.roles.support.LoadableRoleUtils.loadableRoleEntity;
import static org.folio.roles.support.LoadableRoleUtils.regularRole;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.TestUtils.copy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.LoadableRoleMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@UnitTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(InstancioExtension.class)
class LoadableRoleServiceTest {

  @InjectMocks private LoadableRoleService service;
  @Mock private LoadableRoleRepository repository;
  @Mock private LoadableRoleMapper mapper;
  @Mock private KeycloakRoleService keycloakService;
  @Mock private LoadableRoleCapabilityAssignmentHelper capabilityAssignmentHelper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void findByIdOrName_positive() {
    var role = loadableRole();
    var roleEntity = loadableRoleEntity(role);

    when(repository.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.of(roleEntity));
    when(mapper.toRole(roleEntity)).thenReturn(role);

    var actual = service.findByIdOrName(role.getId(), role.getName());

    assertThat(actual).isEqualTo(Optional.of(role));
  }

  @Test
  void findByIdOrName_positive_notFound() {
    var roleId = randomUUID();
    var roleName = insecure().nextAlphabetic(10);

    when(repository.findByIdOrName(roleId, roleName)).thenReturn(Optional.empty());

    var actual = service.findByIdOrName(roleId, roleName);

    assertThat(actual).isEmpty();
  }

  @Test
  void save_positive_whenCreating() {
    var role = loadableRole();
    var newRole = copy(role).id(null);

    var newRoleEntity = loadableRoleEntity(newRole);
    var roleEntity = loadableRoleEntity(role);
    var regularRole = regularRole(role);
    var newRegularRole = copy(regularRole).id(null);

    when(mapper.toRoleEntity(newRole)).thenReturn(newRoleEntity);
    when(mapper.toRegularRole(newRoleEntity)).thenReturn(newRegularRole);
    when(keycloakService.create(newRegularRole)).thenReturn(regularRole);

    when(repository.save(roleEntity)).thenReturn(roleEntity);
    when(mapper.toRole(roleEntity)).thenReturn(role);

    var actual = service.save(newRole);

    assertThat(actual).isEqualTo(role);
  }

  @Test
  void save_positive_whenUpdating() {
    var role = loadableRole();
    var roleEntity = loadableRoleEntity(role);
    var regularRole = regularRole(role);

    when(repository.existsById(role.getId())).thenReturn(true);

    when(mapper.toRoleEntity(role)).thenReturn(roleEntity);
    when(repository.save(roleEntity)).thenReturn(roleEntity);
    when(mapper.toRole(roleEntity)).thenReturn(role);

    when(mapper.toRegularRole(roleEntity)).thenReturn(regularRole);
    when(keycloakService.update(regularRole)).thenReturn(regularRole);

    var actual = service.save(role);

    assertThat(actual).isEqualTo(role);
  }

  @Test
  void save_negative_whenCreating_ifRepositorySaveFailed() {
    var role = loadableRole();
    var newRole = copy(role).id(null);
    var newRoleEntity = loadableRoleEntity(newRole);
    var roleEntity = loadableRoleEntity(role);
    var regularRole = regularRole(role);
    var newRegularRole = copy(regularRole).id(null);

    when(mapper.toRegularRole(newRoleEntity)).thenReturn(newRegularRole);
    when(keycloakService.create(newRegularRole)).thenReturn(regularRole);

    when(mapper.toRoleEntity(newRole)).thenReturn(newRoleEntity);
    when(repository.save(roleEntity)).thenThrow(new DataIntegrityViolationException("Save failed"));

    doNothing().when(keycloakService).deleteById(role.getId());

    assertThatThrownBy(() -> service.save(newRole))
      .isInstanceOf(ServiceException.class)
      .hasMessage("Failed to create loadable role")
      .satisfies(throwable -> {
        var se = (ServiceException) throwable;

        assertThat(se.getKey()).isEqualTo("cause");
        assertThat(se.getValue()).isEqualTo("Save failed");
      });
  }

  @Nested
  @DisplayName("cleanupDefaultRolesFromKeycloak")
  class CleanupDefaultRolesFromKeycloak {

    @Test
    void positive() {
      var role = role();
      var loadableRole = loadableRoleEntity(loadableRole());

      when(repository.findAllByType(EntityRoleType.DEFAULT))
        .thenReturn(Stream.of(loadableRole));
      when(keycloakService.findByName(loadableRole.getName()))
        .thenReturn(Optional.of(role));

      service.cleanupDefaultRolesFromKeycloak();

      verify(keycloakService).deleteByIdSafe(role.getId());
    }

    @Test
    void positive_when_noRoles() {
      when(repository.findAllByType(EntityRoleType.DEFAULT))
        .thenReturn(Stream.empty());

      service.cleanupDefaultRolesFromKeycloak();

      verifyNoInteractions(keycloakService);
    }
  }

  @Nested
  @DisplayName("upsertDefaultLoadableRole")
  class UpsertDefaultLoadableRole {

    @Test
    void positive() {
      var role = loadableRole();
      var roleEntity = loadableRoleEntity(role);
      var createdRegularRole = regularRole(role);
      var regularRole = copy(createdRegularRole).id(null);

      when(repository.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.empty());
      when(mapper.toRoleEntity(role)).thenReturn(roleEntity);
      when(repository.findAllByTypeAndLoadedFromFile(EntityRoleType.DEFAULT, false)).thenReturn(Stream.empty());
      when(mapper.toRegularRole(roleEntity)).thenReturn(regularRole);
      when(keycloakService.findByName(regularRole.getName())).thenReturn(Optional.empty());
      when(keycloakService.create(regularRole)).thenReturn(createdRegularRole);
      when(repository.saveAndFlush(roleEntity)).thenReturn(roleEntity);
      when(capabilityAssignmentHelper.assignCapabilitiesAndSetsForPermissions(roleEntity.getPermissions()))
        .thenReturn(roleEntity.getPermissions());
      when(repository.saveAllAndFlush(org.mockito.ArgumentMatchers.anyList())).thenReturn(java.util.List.of(roleEntity));
      when(repository.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.of(roleEntity));
      when(mapper.toRole(roleEntity)).thenReturn(role);

      var actual = service.upsertDefaultLoadableRole(role);

      assertThat(actual).isEqualTo(role);
    }

    @Test
    void negative_keycloakThrowsException() {
      var role = loadableRole();
      var roleEntity = loadableRoleEntity(role);
      var regularRole = regularRole(role);

      when(repository.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.empty());
      when(mapper.toRoleEntity(role)).thenReturn(roleEntity);
      when(repository.findAllByTypeAndLoadedFromFile(EntityRoleType.DEFAULT, false)).thenReturn(Stream.empty());
      when(mapper.toRegularRole(roleEntity)).thenReturn(regularRole);
      when(keycloakService.findByName(regularRole.getName())).thenReturn(Optional.empty());
      when(keycloakService.create(regularRole)).thenThrow(new RuntimeException("Keycloak error"));

      assertThatThrownBy(() -> service.upsertDefaultLoadableRole(role))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Failed to create loadable roles")
        .satisfies(throwable -> {
          var se = (ServiceException) throwable;

          assertThat(se.getKey()).isEqualTo("cause");
          assertThat(se.getValue()).isEqualTo("Keycloak error");
        });
    }
  }
}
