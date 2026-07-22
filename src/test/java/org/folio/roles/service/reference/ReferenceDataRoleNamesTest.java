package org.folio.roles.service.reference;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.roles.domain.model.PlainLoadableRoles;
import org.folio.roles.utils.JsonHelper;
import org.folio.roles.utils.ResourceHelper;
import org.folio.roles.utils.ResourceHelper.SourcedResource;
import org.folio.roles.utils.RoleNameUtils;
import org.folio.test.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

/**
 * Guards the bundled reference data against role names that Keycloak cannot round-trip through realm
 * export/import. See {@link RoleNameUtils#FORBIDDEN_NAME_CHARACTER}.
 */
@UnitTest
class ReferenceDataRoleNamesTest {

  private static final String ROLES_DATA_DIR = "reference-data/roles";

  private final ResourceHelper resourceHelper = new ResourceHelper(new JsonHelper(TestUtils.OBJECT_MAPPER));

  @Test
  void bundledRoleNames_positive_noForbiddenCharacters() {
    var roles = resourceHelper.readSourcedObjectsFromDirectory(ROLES_DATA_DIR, PlainLoadableRoles.class).toList();

    assertThat(roles).isNotEmpty();
    assertThat(roles).allSatisfy(sourced -> assertThat(sourced.value().getRoles())
      .as("role names in %s", sourced.source())
      .noneMatch(role -> RoleNameUtils.hasForbiddenCharacters(role.getName())));
  }

  @Test
  void bundledRoleNames_positive_matchApiSchemaPattern() {
    var roles = resourceHelper.readSourcedObjectsFromDirectory(ROLES_DATA_DIR, PlainLoadableRoles.class).toList();

    assertThat(roles).isNotEmpty();
    assertThat(roles).allSatisfy(sourced -> assertThat(sourced.value().getRoles())
      .as("role names in %s", sourced.source())
      .allMatch(role -> role.getName().matches(RoleNameUtils.ROLE_NAME_PATTERN)));
  }

  @Test
  void bundledRoleNames_positive_withinDatabaseColumnLength() {
    var roles = resourceHelper.readSourcedObjectsFromDirectory(ROLES_DATA_DIR, PlainLoadableRoles.class).toList();

    assertThat(roles).extracting(SourcedResource::value)
      .flatExtracting(PlainLoadableRoles::getRoles)
      .allSatisfy(role -> assertThat(role.getName()).isNotEmpty().hasSizeLessThanOrEqualTo(255));
  }
}
