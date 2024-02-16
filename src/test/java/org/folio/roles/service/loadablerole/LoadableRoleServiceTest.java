package org.folio.roles.service.loadablerole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.LoadableRoleUtils.loadableRole;
import static org.folio.roles.support.LoadableRoleUtils.loadableRoleEntity;
import static org.folio.roles.support.TestUtils.copy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.mapper.entity.LoadableRoleEntityMapper;
import org.folio.roles.repository.LoadableRoleRepository;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LoadableRoleServiceTest {

  @InjectMocks private LoadableRoleService service;
  @Mock private LoadableRoleRepository repository;
  @Mock private LoadableRoleEntityMapper mapper;
  @Mock private KeycloakRoleService keycloakService;

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
    var roleId = UUID.randomUUID();
    var roleName = RandomStringUtils.randomAlphabetic(10);

    when(repository.findByIdOrName(roleId, roleName)).thenReturn(Optional.empty());

    var actual = service.findByIdOrName(roleId, roleName);

    assertThat(actual).isEmpty();
  }

  @Test
  void save_positive_whenCreating() {
    var role = loadableRole();
    var newRole = copy(role).id(null);
    var roleEntity = loadableRoleEntity(role);

    when(keycloakService.create(newRole)).thenReturn(role);

    when(mapper.toRoleEntity(role)).thenReturn(roleEntity);
    when(repository.save(roleEntity)).thenReturn(roleEntity);
    when(mapper.toRole(roleEntity)).thenReturn(role);

    var actual = service.save(newRole);

    assertThat(actual).isEqualTo(role);
  }

  @Test
  void save_positive_whenUpdating() {
    var role = loadableRole();
    var roleEntity = loadableRoleEntity(role);

    when(repository.existsById(role.getId())).thenReturn(true);

    when(mapper.toRoleEntity(role)).thenReturn(roleEntity);
    when(repository.save(roleEntity)).thenReturn(roleEntity);
    when(mapper.toRole(roleEntity)).thenReturn(role);

    when(keycloakService.update(role)).thenReturn(role);

    var actual = service.save(role);

    assertThat(actual).isEqualTo(role);
  }

  @Test
  void save_negative_whenCreating_ifRepositorySaveFailed() {
    var role = loadableRole();
    var newRole = copy(role).id(null);
    var roleEntity = loadableRoleEntity(role);

    when(keycloakService.create(newRole)).thenReturn(role);

    when(mapper.toRoleEntity(role)).thenReturn(roleEntity);
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
}
