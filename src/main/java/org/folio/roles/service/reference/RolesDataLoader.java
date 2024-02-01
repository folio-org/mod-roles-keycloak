package org.folio.roles.service.reference;

import static java.util.stream.Collectors.toSet;
import static org.folio.common.utils.CollectionUtils.toStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.model.LoadableRoleType;
import org.folio.roles.domain.model.LoadableRoles;
import org.folio.roles.service.role.LoadableRoleService;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.utils.ResourceHelper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class RolesDataLoader implements ReferenceDataLoader {

  private static final String ROLES_DATA_DIR = BASE_DIR + "roles";
  private static final String DEFAULT_ROLES_DATA_DIR = ROLES_DATA_DIR + "default";

  private final RoleService roleService;
  private final LoadableRoleService loadableRoleService;
  private final ResourceHelper resourceHelper;

  @Override
  public void loadReferenceData() {
    loadDefaultRoles();
  }

  private void loadDefaultRoles() {
    var preparedRoles = toStream(resourceHelper.readObjectsFromDirectory(DEFAULT_ROLES_DATA_DIR, LoadableRoles.class))
      .flatMap(roles -> toStream(roles.getRoles()))
      .map(role -> role.type(LoadableRoleType.DEFAULT))
      .collect(toSet());

    for (var role : preparedRoles) {
      if (role.getId() != null && roleService.existById(role.getId())) {
        var updatedRole = roleService.update(role);
        log.info("Role has been updated: id = {}, name = {}", updatedRole.getId(), updatedRole.getName());
      } else {
        var createdRole = roleService.create(role);
        log.info("Role has been created: id = {}, name = {}", createdRole.getId(), createdRole.getName());
        role.setId(createdRole.getId());
      }

      loadableRoleService.save(role);
    }
  }
}
