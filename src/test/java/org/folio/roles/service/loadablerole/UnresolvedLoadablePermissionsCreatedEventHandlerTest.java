package org.folio.roles.service.loadablerole;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UnresolvedLoadablePermissionsCreatedEventHandlerTest {

  @InjectMocks private UnresolvedLoadablePermissionsCreatedEventHandler handler;
  @Mock private LoadablePermissionService loadablePermissionService;

  @Test
  void handle_positive() {
    var permissionKey = LoadablePermissionKey.of(randomUUID(), "permission.test");

    handler.handle(new UnresolvedLoadablePermissionsCreatedEvent(List.of(permissionKey)));

    verify(loadablePermissionService).assignCapabilitiesAndSets(List.of(permissionKey));
  }
}
