package org.folio.roles.it;

import static java.util.Collections.emptyList;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.PermissionsUser;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.util.LinkedMultiValueMap;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = {
  "/sql/capabilities/populate-capabilities.sql",
  "/sql/capability-sets/populate-capability-sets.sql",
  "/sql/populate-user-capability-relations.sql",
  "/sql/capabilities/populate-capability-permissions.sql"
})
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "/sql/truncate-capability-tables.sql",
  "/sql/truncate-role-tables.sql",
  "/sql/truncate-role-capability-tables.sql",
  "/sql/truncate-user-capability-tables.sql",
  "/sql/truncate-roles-user-related-tables.sql",
  "/sql/truncate-permission-table.sql"
})
class PermissionsUserIT extends BaseIntegrationTest {

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @MethodSource("getPermissionsUserDataProvider")
  @DisplayName("getPermissionsUser_parameterized")
  @ParameterizedTest(name = "[{index}] userId={0}, onlyVisible={1}, expectedPermissions={2}, desiredPermissions={3}")
  void getPermissionsUser_parameterized(UUID userId, boolean onlyVisible, List<String> expected, List<String> desired)
    throws Exception {
    var expectedPermissionsUser = new PermissionsUser().userId(userId).permissions(expected);
    var desiredPermissionsParam = new LinkedMultiValueMap<>(Map.of("desiredPermissions", desired));

    mockMvc.perform(get("/permissions/users/{id}", userId)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .queryParam("onlyVisible", String.valueOf(onlyVisible))
        .queryParams(desiredPermissionsParam))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedPermissionsUser)));
  }

  private static Stream<Arguments> getPermissionsUserDataProvider() {
    var userId1 = UUID.fromString("cf078e4a-5d9c-45f1-9c1d-f87003790d9f");
    var userId2 = UUID.fromString("9d30bb2b-8c6d-47da-9726-0e067b65f30b");
    var userId3 = UUID.fromString("bd41e413-21c6-4755-a11c-18da7780e02f");
    var userId4 = UUID.fromString("c2bdde31-e216-43f7-abe3-54b6415d7472");
    var userId5 = UUID.fromString("1cc7f14f-28f9-4110-be6c-51d782d32ba4");
    var userId6 = UUID.fromString("fe0d0b41-c743-44c9-8842-9a190a0cf568");

    // MODROLESKC-333: replaced permissions are not returned when filtering is applied
    return Stream.of(
      arguments(userId1, true, List.of("module.foo.item.post", "plugin.foo.item.get", "ui-foo.item.delete",
          "ui-foo.item.put"),
        List.of("ui-foo.item.*", "module.foo.item.post")),
      arguments(userId1, false, List.of(
        "foo.item.delete", "replaced.foo.item.delete", "foo.item.get", "foo.item.post", "foo.item.put",
        "module.foo.item.post", "plugin.foo.item.get", "ui-foo.item.delete", "ui-foo.item.put",
        "replaced.ui-foo.item.put"), emptyList()),

      arguments(userId2, true, List.of(
        "plugin.foo.item.get", "ui-foo.item.delete", "ui-foo.item.put"), emptyList()),
      arguments(userId2, false, List.of(
        "foo.item.delete", "replaced.foo.item.delete", "foo.item.get", "foo.item.post", "foo.item.put",
        "plugin.foo.item.get", "ui-foo.item.delete", "ui-foo.item.put", "replaced.ui-foo.item.put"), emptyList()),

      arguments(userId3, true, List.of(
        "module.foo.item.post", "plugin.foo.item.get", "ui-foo.item.delete", "ui-foo.item.put"), emptyList()),
      arguments(userId3, false, List.of(
        "module.foo.item.post", "plugin.foo.item.get", "ui-foo.item.delete", "ui-foo.item.put",
        "replaced.ui-foo.item.put"), emptyList()),

      arguments(userId4, true, List.of("ui-foo.item.delete"), emptyList()),
      arguments(userId4, false, List.of(
        "foo.item.delete", "replaced.foo.item.delete", "foo.item.get", "foo.item.post",
        "foo.item.put", "ui-foo.item.delete"), emptyList()),

      arguments(userId5, true, List.of("plugin.foo.item.get", "ui-foo.item.put"),
        emptyList()),
      arguments(userId5, false, List.of(
        "foo.item.delete", "replaced.foo.item.delete", "foo.item.get", "foo.item.post",
        "foo.item.put", "plugin.foo.item.get", "ui-foo.item.put", "replaced.ui-foo.item.put"), emptyList()),

      arguments(userId6, true, emptyList(), emptyList()),
      arguments(userId6, false, emptyList(), emptyList()),
      arguments(userId1, false, List.of(
          "foo.item.delete", "foo.item.get", "foo.item.post", "foo.item.put",
          "ui-foo.item.delete"),
        List.of("foo.item.*", "ui-foo.item.delete")),
      arguments(userId1, false, List.of(
          "foo.item.delete", "foo.item.get", "foo.item.post", "foo.item.put",
          "ui-foo.item.delete", "ui-foo.item.put"),
        List.of("foo.item.*", "ui-foo.item.*")),
      arguments(userId1, false, List.of(
          "foo.item.delete", "ui-foo.item.delete"),
        List.of("foo.item.delete", "ui-foo.item.delete"))
    );
  }
}
