package org.folio.roles.service.reference;

import static java.util.stream.Collectors.toSet;
import static org.folio.common.utils.CollectionUtils.toStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.utils.ResourceHelper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class RolesDataLoader implements ReferenceDataLoader {

  private static final String ROLES_DATA_DIR = BASE_DIR + "roles";

  private final RoleService roleService;
  private final ResourceHelper resourceHelper;

  @Override
  public void loadReferenceData() {
    var preparedRoles = toStream(resourceHelper.readObjectsFromDirectory(ROLES_DATA_DIR, Roles.class))
      .flatMap(roles -> toStream(roles.getRoles()))
      .collect(toSet());

    for (var role : preparedRoles) {
      if (role.getId() != null && roleService.existById(role.getId())) {
        var updatedRole = roleService.update(role);
        log.info("Role has been updated: id = {}, name = {}", updatedRole.getId(), updatedRole.getName());
      } else {
        var createdRole = roleService.create(role);
        log.info("Role has been created: id = {}, name = {}", createdRole.getId(), createdRole.getName());
      }
    }
  }
}
