package org.folio.roles.domain.model.event;

import org.folio.roles.domain.dto.Capability;

public final class CapabilityEvent extends DomainEvent<Capability> {

  private CapabilityEvent(Capability newCapability, Capability oldCapability, DomainEventType type) {
    super(newCapability, oldCapability, type);
  }

  public static CapabilityEvent created(Capability newCapability) {
    return new CapabilityEvent(newCapability, null, DomainEventType.CREATE);
  }

  public static CapabilityEvent updated(Capability newCapability, Capability oldCapability) {
    return new CapabilityEvent(newCapability, oldCapability, DomainEventType.UPDATE);
  }

  public static CapabilityEvent deleted(Capability oldCapability) {
    return new CapabilityEvent(null, oldCapability, DomainEventType.DELETE);
  }
}
