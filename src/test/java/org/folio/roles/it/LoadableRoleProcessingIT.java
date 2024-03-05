package org.folio.roles.it;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.TWO_HUNDRED_MILLISECONDS;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.parseResponse;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.integration.kafka.CapabilityConverterUtils;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.utils.CapabilityUtils;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.jdbc.JdbcTestUtils;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/clean-loadable-permission-capability-fields.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql"
})
public class LoadableRoleProcessingIT extends BaseIntegrationTest {

  private static final UUID CIRC_MANAGER_ROLE_ID = UUID.fromString("5a3a3b6d-ea37-4faf-98fe-91ded163a89e");
  private static final String CIRC_MANAGER_ROLE_NAME = "Circulation Manager";
  private static final UUID CIRC_STUDENT_ROLE_ID = UUID.fromString("c14cfe6f-b971-4117-884c-7b5efd1cf076");
  private static final String CIRC_STUDENT_ROLE_NAME = "Circulation Student";
  private static final String ROLE_LOADABLE_PERMISSION_TABLE = "test_mod_roles_keycloak.role_loadable_permission";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private LoadableRoleService roleService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  @Sql(scripts = {
    "classpath:/sql/set-sql-search-path.sql",
    "classpath:/sql/populate-role-loadable.sql"
  })
  @Disabled
  void findCapabilities_positive() throws Exception {
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-notes-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilitySetCountForPermission("ui-notes");
      assertThat(count).isEqualTo(0);
    });

    var mvcResult = doGet(get("/capability-sets")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andReturn();
    var capabilitySets = parseResponse(mvcResult, CapabilitySets.class).getCapabilitySets();
    assertThat(capabilitySets).isNotEmpty();
    var capSetByName = capabilitySets.stream().collect(toMap(CapabilitySet::getName, identity()));

    var cmRole = roleService.findByIdOrName(CIRC_MANAGER_ROLE_ID, CIRC_MANAGER_ROLE_NAME);
    assertThat(cmRole).isPresent();

    var permissions = cmRole.get().getPermissions();
    var notePermissions = permissions.stream()
      .filter(p -> p.getPermissionName().startsWith("ui-notes"))
      .toList();

    assertThat(notePermissions)
      .isNotEmpty()
      .allSatisfy(p -> {
        assertThat(p.getCapabilitySetId()).isNotNull();

        var name = p.getPermissionName();
        var rawCap = CapabilityConverterUtils.getRawCapability(name);
        var capabilitySetName = CapabilityUtils.getCapabilityName(rawCap);

        var capabilitySet = capSetByName.get(capabilitySetName);
        assertThat(capabilitySet).isNotNull();
        assertThat(p.getCapabilitySetId()).isEqualTo(capabilitySet.getId());
      });
  }

  private int unassignedCapabilitySetCountForPermission(String permission) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, ROLE_LOADABLE_PERMISSION_TABLE,
      format("folio_permission LIKE '%%%s%%' and capability_set_id IS NULL", permission));
  }

  private void sendCapabilityEvent(String file) {
    var capabilityEvent = readValue(file, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(TWO_HUNDRED_MILLISECONDS);
  }

  private static void awaitFor(Duration duration) {
    var sampleResult = Optional.of(1);
    Awaitility.await()
      .pollInSameThread()
      .atMost(duration.plus(Duration.ofMillis(250)))
      .pollDelay(duration)
      .untilAsserted(() -> assertThat(sampleResult).isPresent());
  }
}
