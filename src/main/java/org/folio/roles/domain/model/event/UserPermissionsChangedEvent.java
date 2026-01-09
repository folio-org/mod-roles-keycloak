package org.folio.roles.domain.model.event;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

/**
 * Event published when a specific user's permissions have changed.
 * This triggers cache eviction for that user only.
 *
 * @param userId - the user whose permissions changed
 */
public record UserPermissionsChangedEvent(UUID userId) {

  /**
   * Compact constructor for validation.
   */
  public UserPermissionsChangedEvent {
    requireNonNull(userId, "userId cannot be null for UserPermissionsChangedEvent");
  }

  /**
   * Factory method to create the event.
   *
   * @param userId - the user whose permissions changed
   * @return new event instance
   */
  public static UserPermissionsChangedEvent userPermissionsChanged(UUID userId) {
    return new UserPermissionsChangedEvent(userId);
  }
}
