package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.LoadableRoleType.DEFAULT;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.parseResponse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowingConsumer;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.model.PlainLoadableRoles;
import org.folio.roles.service.reference.PoliciesDataLoader;
import org.folio.roles.utils.ResourceHelper;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

@IntegrationTest
public class ReferenceRoleLoadingIT extends BaseIntegrationTest {

  private static final TenantAttributes TENANT_ATTR = new TenantAttributes()
    .addParametersItem(new Parameter()
      .key("loadReference")
      .value("true"));

  @MockBean
  private PoliciesDataLoader policiesDataLoader;
  @MockBean
  private ResourceHelper resourceHelper;

  @BeforeEach
  void setUp() {
    doNothing().when(policiesDataLoader).loadReferenceData(); // disable policy loading
  }

  @AfterEach
  void afterEach() {
    removeTenant(TENANT_ID);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-ref-data.json")
  void defaultRolesInitialized_positive() throws Exception {
    var circObserverRole = readRole("circ-observer-role.json");
    var circStaffRole = readRole("circ-staff-role.json");
    var circStudentRole = readRole("circ-student-role.json");
    when(resourceHelper.readObjectsFromDirectory(anyString(), eq(PlainLoadableRoles.class)))
      .thenReturn(Stream.of(circObserverRole, circStaffRole, circStudentRole));

    enableTenant(TENANT_ID, TENANT_ATTR);

    var roleResponse = parseResponse(doGet("/loadable-roles").andReturn(), LoadableRoles.class);
    var roles = roleResponse.getLoadableRoles();

    assertThat(roles).satisfiesExactly(
      roleMatches(circObserverRole),
      roleMatches(circStaffRole),
      roleMatches(circStudentRole)
    );
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-ref-data.json")
  void defaultRolesUpgraded_positive_rolesAddedAndDeleted() throws Exception {
    var circAdminRole = readRole("circ-admin-role.json");
    var circObserverRole = readRole("circ-observer-role.json");
    var circStaffRole = readRole("circ-staff-role.json");
    var circStudentRole = readRole("circ-student-role.json");
    when(resourceHelper.readObjectsFromDirectory(anyString(), eq(PlainLoadableRoles.class)))
      .thenReturn(Stream.of(circObserverRole, circStaffRole, circStudentRole))
      .thenReturn(Stream.of(circAdminRole, circObserverRole, circStaffRole)); // admin added, student removed

    enableTenant(TENANT_ID, TENANT_ATTR); // initialize
    enableTenant(TENANT_ID, TENANT_ATTR); // upgrade

    var roleResponse = parseResponse(doGet("/loadable-roles").andReturn(), LoadableRoles.class);
    var roles = roleResponse.getLoadableRoles();

    assertThat(roles).satisfiesExactly(
      roleMatches(circAdminRole),
      roleMatches(circObserverRole),
      roleMatches(circStaffRole)
    );
  }

  private static ThrowingConsumer<LoadableRole> roleMatches(PlainLoadableRoles expectedPlainRole) {
    return role -> {
      var expected = expectedPlainRole.getRoles().get(0);

      assertThat(role.getId()).isNotNull();
      assertThat(role.getType()).isEqualTo(DEFAULT);
      assertThat(role.getName()).isEqualTo(expected.getName());
      assertThat(role.getDescription()).isEqualTo(expected.getDescription());
      assertThat(role.getMetadata()).isNotNull();

      assertThat(role.getPermissions()).satisfies(permissionsMatched(role.getId(), expected.getPermissions()));
    };
  }

  private static ThrowingConsumer<List<? extends LoadablePermission>> permissionsMatched(
    UUID roleId, Set<String> expectedPerms) {
    return loadablePermissions -> {
      assertThat(loadablePermissions.size()).isEqualTo(expectedPerms.size());
      assertThat(loadablePermissions).allSatisfy(perm -> {
        assertThat(perm.getRoleId()).isEqualTo(roleId);
        assertThat(perm.getPermissionName()).isIn(expectedPerms);
        assertThat(perm.getMetadata()).isNotNull();
      });
    };
  }

  private static PlainLoadableRoles readRole(String filename) {
    return readValue("json/reference-data/roles/" + filename, PlainLoadableRoles.class);
  }
}
