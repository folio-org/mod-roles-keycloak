package org.folio.roles.service.loadablerole;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Log4j2
@RequiredArgsConstructor
public class UnresolvedLoadablePermissionsCreatedEventHandler {

  private final LoadablePermissionService loadablePermissionService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener
  public void handle(UnresolvedLoadablePermissionsCreatedEvent event) {
    log.debug("Handling unresolved loadable permissions event: permissionKeys = {}", event.unresolvedPermissionKeys());
    loadablePermissionService.assignCapabilitiesAndSets(event.unresolvedPermissionKeys());
  }
}
