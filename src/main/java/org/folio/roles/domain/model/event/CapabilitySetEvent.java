package org.folio.roles.domain.model.event;

import org.folio.roles.domain.dto.CapabilitySet;

public final class CapabilitySetEvent extends DomainEvent<CapabilitySet> {

  private CapabilitySetEvent(CapabilitySet newObject, CapabilitySet oldObject, DomainEventType type) {
    super(newObject, oldObject, type);
  }

  public static CapabilitySetEvent created(CapabilitySet newObject) {
    return new CapabilitySetEvent(newObject, null, DomainEventType.CREATE);
  }

  public static CapabilitySetEvent updated(CapabilitySet newObject, CapabilitySet oldObject) {
    return new CapabilitySetEvent(newObject, oldObject, DomainEventType.UPDATE);
  }

  public static CapabilitySetEvent deleted(CapabilitySet oldObject) {
    return new CapabilitySetEvent(null, oldObject, DomainEventType.DELETE);
  }
}
