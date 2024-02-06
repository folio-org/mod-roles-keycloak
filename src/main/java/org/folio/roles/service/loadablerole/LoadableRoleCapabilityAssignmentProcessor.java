package org.folio.roles.service.loadablerole;

import java.util.Collection;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.service.event.DomainEvent;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LoadableRoleCapabilityAssignmentProcessor {

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitiesCreatedEvent(DomainEvent<? extends Collection<Capability>> event) {
    try (var x = new FolioExecutionContextSetter(event.getContext())) {
    }
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).CREATE")
  public void handleCapabilitySetCreatedEvent(DomainEvent<CapabilitySet> event) {
    try (var x = new FolioExecutionContextSetter(event.getContext())) {
    }
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).UPDATE")
  public void handleCapabilitySetUpdatedEvent(DomainEvent<CapabilitySet> event) {
    try (var x = new FolioExecutionContextSetter(event.getContext())) {
    }
  }

  @Async
  @TransactionalEventListener(condition = "#event.type == T(org.folio.roles.service.event.DomainEventType).DELETE")
  public void handleCapabilitySetDeletedEvent(DomainEvent<CapabilitySet> event) {
    try (var x = new FolioExecutionContextSetter(event.getContext())) {
    }
  }
}
