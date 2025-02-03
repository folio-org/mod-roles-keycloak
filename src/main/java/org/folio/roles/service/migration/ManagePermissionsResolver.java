package org.folio.roles.service.migration;

import static org.folio.roles.utils.CollectionUtils.union;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.model.CapabilitiesToManageCapabilities;
import org.folio.roles.domain.model.PermissionsToManagePermissions;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.utils.ResourceHelper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class ManagePermissionsResolver {

  private static final String PERMISSIONS_VIEW_EDIT_DIR = "permissions-view-edit-mapping/permissions";
  private static final String CAPABILITIES_VIEW_EDIT_DIR = "permissions-view-edit-mapping/capabilities";

  private final ResourceHelper resourceHelper;
  private final CapabilityService capabilityService;
  @Getter
  private PermissionsToManagePermissions permissionsToManagePermissions;
  @Getter
  private CapabilitiesToManageCapabilities capabilitiesToManageCapabilities;

  @PostConstruct
  void loadPermissionsAndCapabilities() {
    this.permissionsToManagePermissions = resourceHelper
      .readObjectsFromDirectory(PERMISSIONS_VIEW_EDIT_DIR, PermissionsToManagePermissions.class)
      .findFirst()
      .orElse(new PermissionsToManagePermissions());
    this.capabilitiesToManageCapabilities = resourceHelper
      .readObjectsFromDirectory(CAPABILITIES_VIEW_EDIT_DIR, CapabilitiesToManageCapabilities.class)
      .findFirst()
      .orElse(new CapabilitiesToManageCapabilities());
  }

  /**
   * Add to user capabilities to edit and view other capabilities
   * if user has permission to edit and view other permissions.
   *
   * @param userPermissions - user permissions list
   */
  public void addManageCapabilities(List<UserPermissions> userPermissions) {
    var existCapabilitiesToView = getExistCapabilitiesToView();
    var existCapabilitiesToEdit = getExistCapabilitiesToEdit();
    existCapabilitiesToEdit = union(existCapabilitiesToEdit, existCapabilitiesToView);
    for (var userPermission : userPermissions) {
      if (isUserHasEditPermissions(userPermission)) {
        userPermission.manageCapabilities(existCapabilitiesToEdit);
      } else if (isUserHasViewPermissions(userPermission)) {
        userPermission.manageCapabilities(existCapabilitiesToView);
      }
    }
  }

  private List<Capability> getExistCapabilitiesToView() {
    return capabilityService.findByNames(capabilitiesToManageCapabilities.getViewCapabilities());
  }

  private List<Capability> getExistCapabilitiesToEdit() {
    return capabilityService.findByNames(capabilitiesToManageCapabilities.getEditCapabilities());
  }

  private boolean isUserHasEditPermissions(UserPermissions userPermissions) {
    return userPermissions.getPermissions()
      .stream()
      .anyMatch(perm -> permissionsToManagePermissions.getEditPermissions()
        .contains(perm));
  }

  private boolean isUserHasViewPermissions(UserPermissions userPermissions) {
    return userPermissions.getPermissions()
      .stream()
      .anyMatch(perm -> permissionsToManagePermissions.getViewPermissions()
        .contains(perm));
  }
}
