package org.folio.roles.service.loadablerole;

import java.util.Collection;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;

/**
 * Published after the loadable-role transaction commits for newly created permissions that still have neither a
 * capability nor a capability set. {@link UnresolvedLoadablePermissionsCreatedEventHandler} uses these permission
 * keys to perform capability assignment in a new transaction.
 */
public record UnresolvedLoadablePermissionsCreatedEvent(Collection<LoadablePermissionKey> unresolvedPermissionKeys) {
}
