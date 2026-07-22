package org.folio.roles.service.reference;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.dto.RoleType.DEFAULT;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.folio.roles.utils.ResourceHelper.SourcedResource;
import org.folio.spring.FolioExecutionContext;
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

  private static final String ROLES_DIR = "reference-data/roles";

  @InjectMocks private RolesDataLoader rolesDataLoader;
  @Mock private LoadableRoleService roleService;
  @Mock private ResourceHelper resourceHelper;
  @Mock private FolioExecutionContext folioExecutionContext;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void loadReferenceData_positive_ifCreate() {
    var role = new PlainLoadableRole().name("role1");
    var roles = sourced("role1.json", role);
    var loadableRole = new LoadableRole().name(role.getName()).type(DEFAULT).permissions(emptyList());

    when(resourceHelper.readSourcedObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class))
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
    var roles = sourced("role1.json", role);
    var loadableRole = new LoadableRole().id(role.getId()).type(DEFAULT)
      .name(role.getName()).description(role.getDescription())
      .permissions(mapItems(role.getPermissions(), perm -> new LoadablePermission(perm).roleId(role.getId())));

    var existingLoadableRole = new LoadableRole().id(role.getId())
      .name(role.getName()).type(DEFAULT).permissions(emptyList());

    when(resourceHelper.readSourcedObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class))
      .thenReturn(Stream.of(roles));
    when(roleService.findByIdOrName(role.getId(), role.getName())).thenReturn(Optional.of(existingLoadableRole));
    doNothing().when(roleService).saveAll(List.of(loadableRole));

    rolesDataLoader.loadReferenceData();
  }

  @Test
  void loadReferenceData_negative_ifReadError() {
    when(resourceHelper.readSourcedObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class))
      .thenThrow(new IllegalStateException("Failed to deserialize data"));

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData()).isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to deserialize data");
  }

  @Test
  void loadReferenceData_negative_roleNameContainsForbiddenCharacter() {
    var validRole = new PlainLoadableRole().name("role1");
    var invalidRole = new PlainLoadableRole().name("Circulation/Administrator");

    when(resourceHelper.readSourcedObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class))
      .thenReturn(Stream.of(sourced("role1.json", validRole), sourced("circ-admin-role.json", invalidRole)));
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);

    assertThatThrownBy(() -> rolesDataLoader.loadReferenceData())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Role name must not contain '/': name = Circulation/Administrator, "
        + "source = reference-data/roles/circ-admin-role.json");

    verifyNoInteractions(roleService);
  }

  private static SourcedResource<PlainLoadableRoles> sourced(String fileName, PlainLoadableRole role) {
    return new SourcedResource<>(ROLES_DIR + "/" + fileName, new PlainLoadableRoles().roles(List.of(role)));
  }
}
