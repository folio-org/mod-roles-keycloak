package org.folio.roles.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.roles.domain.model.PlainLoadableRoles;
import org.folio.roles.utils.ResourceHelper.SourcedResource;
import org.folio.test.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ResourceHelperTest {

  private static final String ROLES_DIR = "json/reference-data/roles";

  private final ResourceHelper resourceHelper = new ResourceHelper(new JsonHelper(TestUtils.OBJECT_MAPPER));

  @Test
  void readSourcedObjectsFromDirectory_positive() {
    var actual = resourceHelper.readSourcedObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class).toList();

    assertThat(actual).isNotEmpty();
    assertThat(actual).allSatisfy(sourced -> {
      assertThat(sourced.source()).startsWith(ROLES_DIR + "/").endsWith(".json");
      assertThat(sourced.value().getRoles()).isNotEmpty();
    });
    assertThat(actual).extracting(SourcedResource::source)
      .contains(ROLES_DIR + "/circ-admin-role.json");
  }

  @Test
  void readObjectsFromDirectory_positive_returnsSameValuesWithoutSource() {
    var sourced = resourceHelper.readSourcedObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class).toList();

    var actual = resourceHelper.readObjectsFromDirectory(ROLES_DIR, PlainLoadableRoles.class).toList();

    assertThat(actual).isEqualTo(sourced.stream().map(SourcedResource::value).toList());
  }
}
