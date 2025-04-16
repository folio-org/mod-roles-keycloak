package org.folio.roles.service.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.model.CapabilitiesToManageCapabilities;
import org.folio.roles.domain.model.PermissionsToManagePermissions;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.support.TestUtils;
import org.folio.roles.utils.JsonHelper;
import org.folio.roles.utils.ResourceHelper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ManagePermissionsResolverTest {

  @InjectMocks private ManagePermissionsResolver managePermissionsResolver;

  @Spy private ResourceHelper resourceHelper = new ResourceHelper(new JsonHelper(new ObjectMapper()));
  @Mock private CapabilityService capabilityService;

  @BeforeEach
  void setUp() {
    managePermissionsResolver.loadPermissionsAndCapabilities();
    verify(resourceHelper).readObjectsFromDirectory(isA(String.class), eq(PermissionsToManagePermissions.class));
    verify(resourceHelper).readObjectsFromDirectory(isA(String.class), eq(CapabilitiesToManageCapabilities.class));
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void loadPermissionsAndCapabilities_positive() {
    var managedCapabilities = managePermissionsResolver.getCapabilitiesToManageCapabilities();
    var managedPermissions = managePermissionsResolver.getPermissionsToManagePermissions();

    assertThat(managedCapabilities.getViewCapabilities()).hasSize(15);
    assertThat(managedCapabilities.getEditCapabilities()).hasSize(20);
    assertThat(managedPermissions.getViewPermissions()).hasSize(4);
    assertThat(managedPermissions.getEditPermissions()).hasSize(12);
  }

  @Test
  void addManagedCapabilities_positive_ifUserHasEditPermissionShouldHaveEditAndViewCapabilities() {
    var manageCapabilities = managePermissionsResolver.getCapabilitiesToManageCapabilities();
    var editCapability = new Capability();
    editCapability.setName("user-capabilities_collection.execute");
    var viewCapability = new Capability();
    viewCapability.setName("user-capabilities_collection.view");
    var permissionEditNames = List.of("perms.users.item.post");
    var userPermissions = UserPermissions.of(UUID.randomUUID(), null, "permissionsHash", permissionEditNames, null);

    when(capabilityService.findByNames(manageCapabilities.getEditCapabilities())).thenReturn(List.of(editCapability));
    when(capabilityService.findByNames(manageCapabilities.getViewCapabilities())).thenReturn(List.of(viewCapability));

    managePermissionsResolver.addManageCapabilities(List.of(userPermissions));
    var userManageCapabilities = userPermissions.getManageCapabilities();
    assertThat(userManageCapabilities).hasSize(2).contains(editCapability).contains(viewCapability);
  }

  @Test
  void addManagedCapabilities_positive_ifUserHasViewPermissionShouldHaveViewCapabilities() {
    var manageCapabilities = managePermissionsResolver.getCapabilitiesToManageCapabilities();
    var editCapability = new Capability();
    editCapability.setName("user-capabilities_collection.execute");
    var viewCapability = new Capability();
    viewCapability.setName("user-capabilities_collection.view");
    var permissionViewNames = List.of("perms.users.get");
    var userPermissions = UserPermissions.of(UUID.randomUUID(), null, "permissionsHash", permissionViewNames, null);

    when(capabilityService.findByNames(manageCapabilities.getEditCapabilities())).thenReturn(List.of(editCapability));
    when(capabilityService.findByNames(manageCapabilities.getViewCapabilities())).thenReturn(List.of(viewCapability));

    managePermissionsResolver.addManageCapabilities(List.of(userPermissions));
    var userManageCapabilities = userPermissions.getManageCapabilities();
    assertThat(userManageCapabilities).hasSize(1).contains(viewCapability);
  }

  @Test
  void addManagedCapabilities_positive_ifUserNotHaveEditViewPermissions() {
    var manageCapabilities = managePermissionsResolver.getCapabilitiesToManageCapabilities();
    var editCapability = new Capability();
    editCapability.setName("user-capabilities_collection.execute");
    var viewCapability = new Capability();
    viewCapability.setName("user-capabilities_collection.view");
    var permissionNames = List.of("permission.name");
    var userPermissions = UserPermissions.of(UUID.randomUUID(), null, "permissionsHash", permissionNames, null);

    when(capabilityService.findByNames(manageCapabilities.getEditCapabilities())).thenReturn(List.of(editCapability));
    when(capabilityService.findByNames(manageCapabilities.getViewCapabilities())).thenReturn(List.of(viewCapability));

    managePermissionsResolver.addManageCapabilities(List.of(userPermissions));
    assertThat(userPermissions.getManageCapabilities()).isNull();
  }
}
