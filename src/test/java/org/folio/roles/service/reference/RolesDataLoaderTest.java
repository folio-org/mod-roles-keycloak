package org.folio.roles.service.reference;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.dto.RoleType.DEFAULT;
import static org.folio.roles.domain.dto.RoleType.SUPPORT;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.model.PlainLoadableRole;
import org.folio.roles.domain.model.PlainLoadableRoles;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.support.TestUtils;
import org.folio.roles.utils.ResourceHelper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RolesDataLoaderTest {

  @InjectMocks private RolesDataLoader rolesDataLoader;
  @Mock private LoadableRoleService roleService;
  @Mock private ResourceHelper resourceHelper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void loadReferenceData_positive_ifCreate() {
    var role = new PlainLoadableRole().name("role1");
    var roles = new PlainLoadableRoles().roles(List.of(role));
    var loadableRole = new LoadableRole().name(role.getName()).type(DEFAULT).permissions(emptyList());

    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", PlainLoadableRoles.class))
      .thenReturn(Stream.of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.empty());
    doNothing().when(roleService).saveAll(List.of(loadableRole));

    rolesDataLoader.loadReferenceData();
  }

  @Test
  void loadReferenceData_positive_ifUpdate() {
    var role = new PlainLoadableRole().id(randomUUID())
      .name("role1").description("description1")
      .permissions(Set.of("permission1", "permission2"));
    var roles = new PlainLoadableRoles().roles(List.of(role));
    var loadableRole = new LoadableRole().id(role.getId()).type(DEFAULT)
      .name(role.getName()).description(role.getDescription())
      .permissions(mapItems(role.getPermissions(), perm -> new LoadablePermission(perm).roleId(role.getId())));

    var existingLoadableRole = new LoadableRole().id(role.getId())
      .name(role.getName()).type(DEFAULT).permissions(emptyList());

    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", PlainLoadableRoles.class))
      .thenReturn(Stream.of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.of(existingLoadableRole));
    doNothing().when(roleService).saveAll(List.of(loadableRole));

    rolesDataLoader.loadReferenceData();
  }

  @Test
  void loadReferenceData_negative_ifReadError() {
    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", PlainLoadableRoles.class))
      .thenThrow(new IllegalStateException("Failed to deserialize data"));

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData()).isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to deserialize data");
  }

  @Test
  void loadReferenceData_negative_ifTypeMismatch() {
    var role = new PlainLoadableRole().name("role1").type(DEFAULT);
    var roles = new PlainLoadableRoles().roles(List.of(role));
    var loadableRole = new LoadableRole().id(randomUUID()).name(role.getName()).type(SUPPORT);

    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", PlainLoadableRoles.class))
      .thenReturn(Stream.of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.of(loadableRole));

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Loadable role type cannot be changed: original = %s, new = %s", SUPPORT, DEFAULT);
  }
}
