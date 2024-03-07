package org.folio.roles.service.event;

import java.util.Collection;
import org.folio.roles.domain.dto.Capability;

public final class CapabilityCollectionEvent<C extends Collection<Capability>> extends DomainEvent<C> {

  private CapabilityCollectionEvent(C newObject, C oldObject, DomainEventType type) {
    super(newObject, oldObject, type);
  }

  public static <C extends Collection<Capability>> CapabilityCollectionEvent<C> created(C newObject) {
    return new CapabilityCollectionEvent<>(newObject, null, DomainEventType.CREATE);
  }

  public static <C extends Collection<Capability>> CapabilityCollectionEvent<C> updated(C newObject, C oldObject) {
    return new CapabilityCollectionEvent<>(newObject, oldObject, DomainEventType.UPDATE);
  }

  public static <C extends Collection<Capability>> CapabilityCollectionEvent<C> deleted(C oldObject) {
    return new CapabilityCollectionEvent<>(null, oldObject, DomainEventType.DELETE);
  }
}
