package org.folio.roles.service.reference;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.model.LoadableRoleType.DEFAULT;
import static org.folio.roles.domain.model.LoadableRoleType.SUPPORT;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.folio.roles.domain.model.LoadableRole;
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
    var loadableRole = new LoadableRole().name(role.getName()).type(DEFAULT);

    when(roleService.defaultRoleCount()).thenReturn(0);
    when(resourceHelper.readObjectsFromDirectory("reference-data/roles/default", PlainLoadableRoles.class))
      .thenReturn(of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.empty());
    when(roleService.save(loadableRole)).thenReturn(loadableRole);

    rolesDataLoader.loadReferenceData();
  }

  @Test
  void loadReferenceData_positive_ifUpdate() {
    when(roleService.defaultRoleCount()).thenReturn(new Random().nextInt(1, 100));

    rolesDataLoader.loadReferenceData();
  }

  @Test
  void loadReferenceData_positive_createIfNotExist() {
    var role = new PlainLoadableRole().name("role1").id(randomUUID());
    var roles = new PlainLoadableRoles().roles(List.of(role));
    var loadableRole = new LoadableRole().id(role.getId()).name(role.getName()).type(DEFAULT);

    when(roleService.defaultRoleCount()).thenReturn(0);
    when(resourceHelper.readObjectsFromDirectory("reference-data/roles/default", PlainLoadableRoles.class))
      .thenReturn(of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.empty());
    when(roleService.save(loadableRole)).thenReturn(loadableRole);

    rolesDataLoader.loadReferenceData();
  }

  @Test
  void loadReferenceData_negative_ifReadError() {
    when(roleService.defaultRoleCount()).thenReturn(0);
    when(resourceHelper.readObjectsFromDirectory("reference-data/roles/default", PlainLoadableRoles.class))
      .thenThrow(new IllegalStateException("Failed to deserialize data"));

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData()).isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to deserialize data");
  }

  @Test
  void loadReferenceData_negative_ifTypeMismatch() {
    var role = new PlainLoadableRole().name("role1").type(DEFAULT);
    var roles = new PlainLoadableRoles().roles(List.of(role));
    var loadableRole = new LoadableRole().id(randomUUID()).name(role.getName()).type(SUPPORT);

    when(roleService.defaultRoleCount()).thenReturn(0);
    when(resourceHelper.readObjectsFromDirectory("reference-data/roles/default", PlainLoadableRoles.class))
      .thenReturn(of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.of(loadableRole));

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Loadable role type cannot be changed: original = %s, new = %s", SUPPORT, DEFAULT);
  }
}
