package org.folio.roles.repository.projection;

import java.util.UUID;

/**
 * Projection for a role capability row tagged with its origin (direct assignment vs. capability set).
 */
public interface CapabilityDirectProjection {

  /**
   * Returns the capability identifier.
   */
  UUID getId();

  /**
   * Returns {@code true} if the capability was directly assigned to the role,
   * {@code false} if it was inherited via a capability set.
   */
  Boolean getDirect();
}
