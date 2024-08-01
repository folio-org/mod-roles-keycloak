package org.folio.roles.service.reference;

import static java.util.Objects.requireNonNull;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.model.PlainLoadableRole;
import org.folio.roles.domain.model.PlainLoadableRoles;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.utils.ResourceHelper;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class RolesDataLoader implements ReferenceDataLoader {

  private static final String ROLES_DATA_DIR = BASE_DIR + "roles/";
  private static final String DEFAULT_ROLES_DATA_DIR = ROLES_DATA_DIR + "default";

  private final LoadableRoleService service;
  private final ResourceHelper resourceHelper;

  @Override
  public void loadReferenceData() {
    var incoming = resourceHelper.readObjectsFromDirectory(DEFAULT_ROLES_DATA_DIR, PlainLoadableRoles.class)
      .flatMap(roles -> toStream(roles.getRoles()))
      .map(this::convertToLoadableRole)
      .toList();

    service.saveAll(incoming);
  }

  private LoadableRole convertToLoadableRole(PlainLoadableRole plainRole) {
    return service.findByIdOrName(plainRole.getId(), plainRole.getName())
      .map(copyDataFrom(plainRole))
      .orElseGet(toLoadableRole(plainRole));
  }

  private static Function<LoadableRole, LoadableRole> copyDataFrom(PlainLoadableRole source) {
    return target -> {
      if (target.getType() != source.getType()) {
        throw new IllegalArgumentException("Loadable role type cannot be changed: original = " + target.getType()
          + ", new = " + source.getType());
      }
      target.setName(source.getName());
      target.setDescription(source.getDescription());

      target.setPermissions(toLoadablePerms(target.getId(), source.getPermissions()));

      return target;
    };
  }

  private static Supplier<LoadableRole> toLoadableRole(PlainLoadableRole source) {
    requireNonNull(source.getType());

    return () -> new LoadableRole()
      .id(source.getId())
      .name(source.getName())
      .description(source.getDescription())
      .type(source.getType())
      .permissions(toLoadablePerms(source.getId(), source.getPermissions()))
      .metadata(source.getMetadata());
  }

  private static List<LoadablePermission> toLoadablePerms(UUID roleId, Set<String> permissions) {
    return toStream(permissions)
      .map(permName -> new LoadablePermission().roleId(roleId).permissionName(permName))
      .toList();
  }
}
