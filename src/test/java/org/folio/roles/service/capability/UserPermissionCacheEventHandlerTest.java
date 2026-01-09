package org.folio.roles.service.capability;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.folio.roles.domain.model.event.TenantPermissionsChangedEvent;
import org.folio.roles.domain.model.event.UserPermissionsChangedEvent;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserPermissionCacheEventHandlerTest {

  @Mock private UserPermissionsCacheEvictor userPermissionsCacheEvictor;
  @InjectMocks private UserPermissionCacheEventHandler handler;

  @Test
  void handleUserPermissionsChanged_positive() {
    var userId = UUID.randomUUID();
    var event = UserPermissionsChangedEvent.userPermissionsChanged(userId);

    handler.handleUserPermissionsChanged(event);

    verify(userPermissionsCacheEvictor).evictUserPermissions(userId);
  }

  @Test
  void handleTenantPermissionsChanged_positive() {
    var event = TenantPermissionsChangedEvent.tenantPermissionsChanged();

    handler.handleTenantPermissionsChanged(event);

    verify(userPermissionsCacheEvictor).evictUserPermissionsForCurrentTenant();
  }
}
