package org.folio.roles.service.reference;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.domain.dto.RoleType.DEFAULT;
import static org.folio.roles.utils.RoleNameUtils.FORBIDDEN_NAME_CHARACTER;
import static org.folio.roles.utils.RoleNameUtils.hasForbiddenCharacters;

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
import org.folio.roles.utils.ResourceHelper.SourcedResource;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class RolesDataLoader implements ReferenceDataLoader {

  private static final String ROLES_DATA_DIR = BASE_DIR + "roles";

  private final LoadableRoleService service;
  private final ResourceHelper resourceHelper;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public void loadReferenceData() {
    var sourcedRoles =
      resourceHelper.readSourcedObjectsFromDirectory(ROLES_DATA_DIR, PlainLoadableRoles.class).toList();

    sourcedRoles.forEach(this::validateRoleNames);

    var incoming = sourcedRoles.stream()
      .flatMap(sourced -> toStream(sourced.value().getRoles()))
      .map(role -> role.type(getIfNull(role.getType(), DEFAULT)))
      .map(this::convertToLoadableRole)
      .toList();

    service.saveAll(incoming);
  }

  private void validateRoleNames(SourcedResource<PlainLoadableRoles> sourced) {
    for (var role : emptyIfNull(sourced.value().getRoles())) {
      var name = role.getName();
      
      if (hasForbiddenCharacters(name)) {
        log.error("Role name contains a forbidden character: name = {}, source = {}, tenant = {}",
          name, sourced.source(), folioExecutionContext.getTenantId());

        throw new IllegalArgumentException(format("Role name must not contain '%s': name = %s, source = %s",
          FORBIDDEN_NAME_CHARACTER, name, sourced.source()));
      }
    }
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
