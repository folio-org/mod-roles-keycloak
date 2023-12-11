package org.folio.roles.service.reference;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.utils.ResourceHelper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RolesDataLoaderTest {

  @InjectMocks private RolesDataLoader rolesDataLoader;
  @Mock private RoleService roleService;
  @Mock private ResourceHelper resourceHelper;

  @Test
  void loadReferenceData_positive_ifCreate() {
    var role = new Role().name("role1");
    var roles = new Roles().roles(of(role));

    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", Roles.class))
      .thenReturn(of(roles));
    when(roleService.create(role)).thenReturn(role);

    rolesDataLoader.loadReferenceData();

    verify(resourceHelper).readObjectsFromDirectory("reference-data/roles", Roles.class);
    verify(roleService).create(role);
  }

  @Test
  void loadReferenceData_positive_ifUpdate() {
    var role = new Role().name("role1").id(randomUUID());
    var roles = new Roles().roles(of(role));

    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", Roles.class))
      .thenReturn(of(roles));
    when(roleService.update(role)).thenReturn(role);
    when(roleService.existById(role.getId())).thenReturn(true);

    rolesDataLoader.loadReferenceData();

    verify(resourceHelper).readObjectsFromDirectory("reference-data/roles", Roles.class);
    verify(roleService).update(role);
    verify(roleService).existById(role.getId());
  }

  @Test
  void loadReferenceData_positive_createIfNotExist() {
    var role = new Role().name("role1").id(randomUUID());
    var roles = new Roles().roles(of(role));

    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", Roles.class))
      .thenReturn(of(roles));
    when(roleService.create(role)).thenReturn(role);
    when(roleService.existById(role.getId())).thenReturn(false);

    rolesDataLoader.loadReferenceData();

    verify(resourceHelper).readObjectsFromDirectory("reference-data/roles", Roles.class);
    verify(roleService).create(role);
    verify(roleService).existById(role.getId());
  }

  @Test
  void loadReferenceData_negative_ifError() {
    when(resourceHelper.readObjectsFromDirectory("reference-data/roles", Roles.class))
      .thenThrow(new IllegalStateException("Failed to deserialize data"));

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData()).isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to deserialize data");

    verify(resourceHelper).readObjectsFromDirectory("reference-data/roles", Roles.class);
    verifyNoInteractions(roleService);
  }
}
