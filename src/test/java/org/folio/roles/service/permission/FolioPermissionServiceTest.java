package org.folio.roles.service.permission;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.AuthResourceUtils.fooPermission;
import static org.folio.roles.support.AuthResourceUtils.fooPermissionEntity;
import static org.folio.roles.support.AuthResourceUtils.fooPermissionEntityV2;
import static org.folio.roles.support.AuthResourceUtils.fooPermissionV2;
import static org.folio.roles.support.AuthResourceUtils.permissionEntity;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.mapper.entity.PermissionEntityMapper;
import org.folio.roles.repository.PermissionRepository;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class FolioPermissionServiceTest {

  @InjectMocks private FolioPermissionService service;
  @Mock private PermissionRepository repository;
  @Mock private PermissionEntityMapper mapper;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(repository, mapper);
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive_cleanInstallation() {
      var entityId = UUID.randomUUID();
      var permission = fooPermission(null);
      var entity = fooPermissionEntity(null);
      var savedEntity = fooPermissionEntity(entityId);

      when(repository.findByPermissionNameIn(List.of(permission.getPermissionName()))).thenReturn(emptyList());
      when(mapper.toEntity(permission)).thenReturn(entity);
      when(repository.saveAll(List.of(entity))).thenReturn(List.of(savedEntity));

      service.update(List.of(permission), emptyList());

      verify(repository).saveAll(List.of(entity));
    }

    @Test
    void positive_entityExists() {
      var entityId = UUID.randomUUID();
      var permission = fooPermission(null);
      var entity = fooPermissionEntity(null);
      var permissionEntity = fooPermissionEntity(entityId);
      var permissionNames = List.of(permission.getPermissionName());

      when(repository.findByPermissionNameIn(permissionNames)).thenReturn(List.of(permissionEntity));
      when(mapper.toEntity(permission)).thenReturn(entity);
      when(repository.saveAll(List.of(permissionEntity))).thenReturn(List.of(permissionEntity));

      service.update(List.of(permission), emptyList());

      verify(repository).saveAll(List.of(permissionEntity));
    }

    @Test
    void positive_entityUpdated() {
      var entityId = UUID.randomUUID();
      var entityId2 = UUID.randomUUID();
      var permission = fooPermission(null);
      var permissionV2 = fooPermissionV2(null);
      var permissionEntity = fooPermissionEntity(entityId);
      var permissionEntity2 = fooPermissionEntity(entityId2);
      var permissionEntityV2 = fooPermissionEntityV2(null);
      var permissionNames = List.of(permission.getPermissionName());

      when(repository.findByPermissionNameIn(permissionNames)).thenReturn(List.of(permissionEntity, permissionEntity2));
      when(mapper.toEntity(permissionV2)).thenReturn(permissionEntityV2);
      when(repository.saveAll(List.of(permissionEntityV2))).thenReturn(List.of(fooPermissionEntityV2(entityId)));

      service.update(List.of(permissionV2), List.of(permission));

      verify(repository).saveAll(List.of(permissionEntityV2));
      // If we have duplicate permission entries in DB (different IDs, same permission name),
      // any one of these should be used
      assertThat(permissionEntityV2.getId()).isIn(entityId, entityId2);
      // The other one should be deleted
      verify(repository).delete(permissionEntityV2.getId() == entityId ? permissionEntity2 : permissionEntity);
    }

    @Test
    void positive_entityDeprecated() {
      var permission = fooPermission(null);
      service.update(emptyList(), List.of(permission));
      verify(repository).deleteAllByPermissionNameIn(List.of(permission.getPermissionName()));
    }

    @Test
    void positive_emptyInput() {
      service.update(emptyList(), emptyList());
      verifyNoMoreInteractions(repository);
    }
  }

  @Nested
  @DisplayName("expandPermissionNames")
  class ExpandPermissionNames {

    @Test
    void positive_notingToExpand() {
      var name = "foo.item.get";
      var foundEntity = permissionEntity(UUID.randomUUID(), name);
      when(repository.findByPermissionNameIn(Set.of(name))).thenReturn(List.of(foundEntity));

      var result = service.expandPermissionNames(Set.of(name));

      assertThat(result).containsExactly(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ui", "module", "plugin", "settings"})
    @DisplayName("positive_expandUiPermissionSet_parametrized")
    void positive_expandUiPermissionSet(String uiPrefix) {
      var subPermissions = List.of("foo.item.get", "foo.item.post");
      var sourcePermissionName = uiPrefix + "-foo.item.create";
      var rootEntity =
        permissionEntity(UUID.randomUUID(), sourcePermissionName, subPermissions.toArray(String[]::new));
      var fooItemGetEntity = permissionEntity(UUID.randomUUID(), "foo.item.get");
      var fooItemPostEntity = permissionEntity(UUID.randomUUID(), "foo.item.post");

      when(repository.findByPermissionNameIn(Set.of(sourcePermissionName))).thenReturn(List.of(rootEntity));
      when(repository.findByPermissionNameIn(new LinkedHashSet<>(subPermissions)))
        .thenReturn(List.of(fooItemGetEntity, fooItemPostEntity));

      var result = service.expandPermissionNames(List.of(sourcePermissionName));

      assertThat(result).containsExactlyInAnyOrder(sourcePermissionName, "foo.item.get", "foo.item.post");
    }

    @Test
    void positive_visitedPermissionSetMustBeIgnored() {
      var sourcePermissionName = "foo.item.all";
      var subPermissions = List.of(sourcePermissionName, "foo.item.get");
      var rootEntity =
        permissionEntity(UUID.randomUUID(), sourcePermissionName, subPermissions.toArray(String[]::new));
      var fooItemGetEntity = permissionEntity(UUID.randomUUID(), "foo.item.get");

      when(repository.findByPermissionNameIn(Set.of(sourcePermissionName))).thenReturn(List.of(rootEntity));
      when(repository.findByPermissionNameIn(Set.of("foo.item.get"))).thenReturn(List.of(fooItemGetEntity));

      var result = service.expandPermissionNames(List.of(sourcePermissionName));

      assertThat(result).containsExactly(sourcePermissionName, "foo.item.get");
    }

    @Test
    void positive_unresolvedSubPermissionNameIsKept() {
      var sourcePermissionName = "foo.item.all";
      var unresolvedName = "bar.item.get";
      var rootEntity = permissionEntity(UUID.randomUUID(), sourcePermissionName, unresolvedName);

      when(repository.findByPermissionNameIn(Set.of(sourcePermissionName))).thenReturn(List.of(rootEntity));
      when(repository.findByPermissionNameIn(Set.of(unresolvedName))).thenReturn(emptyList());

      var result = service.expandPermissionNames(List.of(sourcePermissionName));

      assertThat(result).containsExactly(sourcePermissionName, unresolvedName);
    }

    @Test
    void positive_unresolvedNestedSubPermissionNameIsKept() {
      var sourcePermissionName = "foo.item.all";
      var nestedSetName = "foo.item.execute";
      var unresolvedName = "bar.item.get";
      var rootEntity = permissionEntity(UUID.randomUUID(), sourcePermissionName, nestedSetName);
      var nestedSetEntity = permissionEntity(UUID.randomUUID(), nestedSetName, unresolvedName);

      when(repository.findByPermissionNameIn(Set.of(sourcePermissionName))).thenReturn(List.of(rootEntity));
      when(repository.findByPermissionNameIn(Set.of(nestedSetName))).thenReturn(List.of(nestedSetEntity));
      when(repository.findByPermissionNameIn(Set.of(unresolvedName))).thenReturn(emptyList());

      var result = service.expandPermissionNames(List.of(sourcePermissionName));

      assertThat(result).containsExactly(sourcePermissionName, nestedSetName, unresolvedName);
    }

    @Test
    void positive_emptyPermission() {
      var result = service.expandPermissionNames(emptyList());
      assertThat(result).isEmpty();
    }
  }
}
