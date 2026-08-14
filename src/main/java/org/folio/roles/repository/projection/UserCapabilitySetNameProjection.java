package org.folio.roles.repository.projection;

import java.util.UUID;

/**
 * Projection for effective user capability-set name queries.
 */
public interface UserCapabilitySetNameProjection {

  /**
   * Returns the user identifier.
   *
   * @return user identifier
   */
  UUID getUserId();

  /**
   * Returns the capability-set name.
   *
   * @return capability-set name
   */
  String getCapabilitySetName();
}
