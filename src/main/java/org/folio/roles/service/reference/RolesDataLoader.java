package org.folio.roles.service.reference;

import static java.util.stream.Collectors.toSet;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.model.LoadablePermission;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.domain.model.LoadableRoleType;
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
    loadDefaultRoles();
  }

  private void loadDefaultRoles() {
    toStream(resourceHelper.readObjectsFromDirectory(DEFAULT_ROLES_DATA_DIR, PlainLoadableRoles.class))
      .flatMap(roles -> toStream(roles.getRoles()))
      .map(role -> role.type(LoadableRoleType.DEFAULT))
      .forEach(updateOrCreateRole());
  }

  private Consumer<PlainLoadableRole> updateOrCreateRole() {
    return plainRole -> {
      var toSave = service.findByIdOrName(plainRole.getId(), plainRole.getName())
        .map(copyDataFrom(plainRole))
        .orElseGet(withId(toLoadableRole(plainRole)));

      service.save(toSave);
    };
  }

  private static Function<LoadableRole, LoadableRole> copyDataFrom(PlainLoadableRole source) {
    return target -> {
      if (target.getType() != source.getType()) {
        throw new IllegalArgumentException("Loadable role type cannot be changed: original = " + target.getType() +
          ", new = " + source.getType());
      }
      target.setName(source.getName());
      target.setDescription(source.getDescription());

      target.setPermissions(toLoadablePerms(source.getPermissions()));

      return target;
    };
  }

  private static LoadableRole toLoadableRole(PlainLoadableRole source) {
    return LoadableRole.builder()
      .id(source.getId())
      .name(source.getName())
      .description(source.getDescription())
      .type(source.getType())
      .permissions(toLoadablePerms(source.getPermissions()))
      .metadata(source.getMetadata())
      .build();
  }

  private static Set<LoadablePermission> toLoadablePerms(Set<String> permissions) {
    return toStream(permissions).map(LoadablePermission::of).collect(toSet());
  }

  private static Supplier<LoadableRole> withId(LoadableRole role) {
    return () -> {
      if (role.getId() == null) {
        role.setId(UUID.randomUUID());
      }

      return role;
    };
  }
}
