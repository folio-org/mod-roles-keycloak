package org.folio.roles.domain.model.event;

import org.folio.roles.domain.model.ExtendedCapabilitySet;

public final class CapabilitySetEvent extends DomainEvent<ExtendedCapabilitySet> {

  /**
   * Default constructor for {@link CapabilitySetEvent} object.
   *
   * @param newCapabilitySet - created capability set value
   * @param oldCapabilitySet - previous or deprecated version of capability set
   * @param type - domain event type
   */
  private CapabilitySetEvent(ExtendedCapabilitySet newCapabilitySet, ExtendedCapabilitySet oldCapabilitySet,
    DomainEventType type) {
    super(newCapabilitySet, oldCapabilitySet, type);
  }

  /**
   * A domain event for created capability set.
   *
   * @param newCapabilitySet - created capability set value
   * @return created {@link CapabilitySetEvent} object.
   */
  public static CapabilitySetEvent created(ExtendedCapabilitySet newCapabilitySet) {
    return new CapabilitySetEvent(newCapabilitySet, null, DomainEventType.CREATE);
  }

  /**
   * A domain event for updated capability set.
   *
   * @param newSet - created capability set value
   * @param oldSet - previous version of capability set
   * @return created {@link CapabilitySetEvent} object.
   */
  public static CapabilitySetEvent updated(ExtendedCapabilitySet newSet, ExtendedCapabilitySet oldSet) {
    return new CapabilitySetEvent(newSet, oldSet, DomainEventType.UPDATE);
  }

  /**
   * A domain event for deprecated capability set.
   *
   * @param deprecatedCapabilitySet - deprecated capability set value
   * @return created {@link CapabilitySetEvent} object.
   */
  public static CapabilitySetEvent deleted(ExtendedCapabilitySet deprecatedCapabilitySet) {
    return new CapabilitySetEvent(null, deprecatedCapabilitySet, DomainEventType.DELETE);
  }
}
