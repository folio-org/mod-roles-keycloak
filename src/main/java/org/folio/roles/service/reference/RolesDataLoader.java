package org.folio.roles.service.reference;

import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.model.LoadableRole;
import org.folio.roles.domain.model.LoadableRoleType;
import org.folio.roles.domain.model.LoadableRoles;
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
    toStream(resourceHelper.readObjectsFromDirectory(DEFAULT_ROLES_DATA_DIR, LoadableRoles.class))
      .flatMap(roles -> toStream(roles.getRoles()))
      .map(role -> role.type(LoadableRoleType.DEFAULT))
      .forEach(updateOrCreateRole());
  }

  private Consumer<LoadableRole> updateOrCreateRole() {
    return role -> {
      var toSave = service.findByIdOrName(role.getId(), role.getName())
        .map(copyDataFrom(role))
        .orElseGet(withId(role));

      service.save(toSave);
    };
  }

  private static Function<LoadableRole, LoadableRole> copyDataFrom(LoadableRole source) {
    return target -> {
      if (target.getType() != source.getType()) {
        throw new IllegalArgumentException("Loadable role type cannot be changed: original = " + target.getType() +
          ", new = " + source.getType());
      }
      target.setName(source.getName());
      target.setDescription(source.getDescription());
      target.setPermissions(source.getPermissions());
      return target;
    };
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
