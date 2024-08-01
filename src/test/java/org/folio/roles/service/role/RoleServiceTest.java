package org.folio.roles.service.role;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.ROLE_NAME;
import static org.folio.roles.support.RoleUtils.role;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private KeycloakRoleService keycloakService;
  @Mock private RoleEntityService entityService;

  @InjectMocks private RoleService facade;

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    void positive() {
      var role = role();
      role.setSource(SourceType.SYSTEM);
      when(entityService.getById(ROLE_ID)).thenReturn(role);

      var result = facade.getById(ROLE_ID);

      assertEquals(ROLE_ID, result.getId());
      assertEquals(ROLE_NAME, result.getName());
      assertEquals(SourceType.SYSTEM, result.getSource());
      verifyNoMoreInteractions(entityService);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void create() {
      var role = role();
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenReturn(role);

      facade.create(role);

      verify(keycloakService).create(role);
      verify(entityService).create(role);
    }

    @Test
    void negative_create() {
      var role = role();
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenThrow(RuntimeException.class);

      assertThrows(ServiceException.class, () -> facade.create(role));
      verify(keycloakService).deleteById(role.getId());
    }

    @Test
    void positive() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.createSafe(role)).thenReturn(Optional.of(role));
      when(entityService.create(role)).thenReturn(role).thenThrow(RuntimeException.class);

      var result = facade.create(roles);
      facade.create(roles);

      assertEquals(result.getRoles().size(), result.getTotalRecords());
      assertEquals(result.getRoles().get(0), role);
      verify(keycloakService, times(2)).createSafe(role);
      verify(entityService, times(2)).create(role);
    }

    @Test
    void positive_returnsEmpty() {
      var role = role();

      when(keycloakService.createSafe(role)).thenReturn(Optional.empty());

      var result = facade.create(List.of(role));

      assertTrue(result.getRoles().isEmpty());
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var role = role();

      facade.update(role);

      verify(entityService).update(role);
      verify(keycloakService).update(role);
    }

    @Test
    void negative_repositoryThrowsException() {
      var role = role();

      when(keycloakService.getById(any())).thenReturn(role);
      when(entityService.update(role)).thenThrow(RuntimeException.class);

      assertThrows(RuntimeException.class, () -> facade.update(role));
      verify(keycloakService, times(2)).update(any());
    }
  }

  @Nested
  @DisplayName("updateFoundByName")
  class UpdateByName {

    @Test
    void positive() {
      var role = role();
      when(entityService.create(role)).thenReturn(role);

      facade.updateFoundByName(role);

      verify(keycloakService).update(role);
      verify(entityService).create(role);
    }

    @Test
    void negative_repositoryThrowsException() {
      var role = role();

      when(keycloakService.update(any())).thenReturn(role);
      when(entityService.create(role)).thenThrow(RuntimeException.class);

      assertThrows(ServiceException.class, () -> facade.updateFoundByName(role));
      verify(keycloakService).update(any());
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive() {
      var role = role();
      var roles = List.of(role);
      var cqlQuery = "cql.allRecords = 1";

      when(entityService.findByQuery(cqlQuery, 0, 1)).thenReturn(roles);

      var result = facade.search(cqlQuery, 0, 1);

      assertEquals(result.getTotalRecords(), result.getRoles().size());
      verify(entityService).findByQuery(cqlQuery, 0, 1);
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      facade.deleteById(ROLE_ID);

      verify(entityService).deleteById(ROLE_ID);
      verify(keycloakService).deleteById(ROLE_ID);
    }

    @Test
    void negative_repositoryThrowsException() {
      var role = role();

      when(keycloakService.getById(ROLE_ID)).thenReturn(role);
      doThrow(RuntimeException.class).when(keycloakService).deleteById(ROLE_ID);

      assertThrows(RuntimeException.class, () -> facade.deleteById(ROLE_ID));
      verify(entityService).deleteById(ROLE_ID);
      verify(entityService).create(role);
    }
  }

  @Nested
  @DisplayName("findByIds")
  class FindByIds {

    @Test
    void positive() {
      var roleIds = List.of(ROLE_ID);
      when(entityService.findByIds(roleIds)).thenReturn(List.of(role()));
      var result = facade.findByIds(roleIds);
      assertThat(result).containsExactly(role());
    }

    @Test
    void negative_notAllRolesFound() {
      var roleIds = List.of(ROLE_ID);
      when(entityService.findByIds(roleIds)).thenReturn(emptyList());
      assertThatThrownBy(() -> facade.findByIds(roleIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageMatching("Roles are not found for ids: \\[.*]");
    }
  }
}
