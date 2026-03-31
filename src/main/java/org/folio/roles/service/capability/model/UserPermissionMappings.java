package org.folio.roles.service.capability.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable holder for user permission data including permission-to-application mappings.
 *
 * @param permissions              flat list of all permission names (including replaced ones)
 * @param permissionToApplicationId map from permission name to the application ID that owns it
 */
public record UserPermissionMappings(List<String> permissions, Map<String, String> permissionToApplicationId) {}
