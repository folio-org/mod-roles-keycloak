package org.folio.roles.service.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.repository.projection.UserPermissionApplicationProjection;
import org.folio.roles.service.capability.model.UserPermissionMappings;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserPermissionCacheServiceTest {

  @Mock private CapabilityRepository capabilityRepository;
  @InjectMocks private UserPermissionCacheService userPermissionCacheService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  void getUserPermissionMappings_positive_includesReplacedPermissions() {
    var row = mock(UserPermissionApplicationProjection.class);
    when(row.getPermission()).thenReturn("foo.item.delete");
    when(row.getApplicationId()).thenReturn("app-a-1.0.0");
    when(row.getReplaces()).thenReturn(new String[] {"replaced.foo.item.delete"});
    when(capabilityRepository.findAllUserPermissionMappings(userId)).thenReturn(List.of(row));

    UserPermissionMappings result = userPermissionCacheService.getUserPermissionMappings(userId);

    assertThat(result.permissions()).contains("foo.item.delete", "replaced.foo.item.delete");
    assertThat(result.permissionToApplicationId()).containsEntry("replaced.foo.item.delete", "app-a-1.0.0");
  }

  @Test
  void getUserPermissionMappings_positive_noReplacedPermissions() {
    var row = mock(UserPermissionApplicationProjection.class);
    when(row.getPermission()).thenReturn("foo.item.get");
    when(row.getApplicationId()).thenReturn("app-b-2.0.0");
    when(row.getReplaces()).thenReturn(null);
    when(capabilityRepository.findAllUserPermissionMappings(userId)).thenReturn(List.of(row));

    UserPermissionMappings result = userPermissionCacheService.getUserPermissionMappings(userId);

    assertThat(result.permissions()).containsExactly("foo.item.get");
    assertThat(result.permissionToApplicationId()).containsOnlyKeys("foo.item.get");
  }

  @Test
  void getUserPermissionMappings_positive_emptyResult() {
    when(capabilityRepository.findAllUserPermissionMappings(userId)).thenReturn(List.of());

    UserPermissionMappings result = userPermissionCacheService.getUserPermissionMappings(userId);

    assertThat(result.permissions()).isEmpty();
    assertThat(result.permissionToApplicationId()).isEmpty();
  }
}
