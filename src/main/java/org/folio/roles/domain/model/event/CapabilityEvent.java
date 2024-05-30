package org.folio.roles.domain.model.event;

import org.folio.roles.domain.dto.Capability;

public final class CapabilityEvent extends DomainEvent<Capability> {

  /**
   * Default constructor for {@link CapabilityEvent} object.
   *
   * @param newCapability - created capability value
   * @param oldCapability - previous or deprecated version of capability
   * @param type - domain event type
   */
  private CapabilityEvent(Capability newCapability, Capability oldCapability, DomainEventType type) {
    super(newCapability, oldCapability, type);
  }

  /**
   * A domain event for created capability.
   *
   * @param newCapability - created capability value
   * @return created {@link CapabilityEvent} object.
   */
  public static CapabilityEvent created(Capability newCapability) {
    return new CapabilityEvent(newCapability, null, DomainEventType.CREATE);
  }

  /**
   * A domain event for updated capability.
   *
   * @param newCapability - created capability value
   * @param oldCapability - previous version of capability
   * @return created {@link CapabilityEvent} object.
   */
  public static CapabilityEvent updated(Capability newCapability, Capability oldCapability) {
    return new CapabilityEvent(newCapability, oldCapability, DomainEventType.UPDATE);
  }

  /**
   * A domain event for deprecated capability.
   *
   * @param oldCapability - deprecated capability value
   * @return created {@link CapabilityEvent} object.
   */
  public static CapabilityEvent deleted(Capability oldCapability) {
    return new CapabilityEvent(null, oldCapability, DomainEventType.DELETE);
  }
}
