package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.parseResponse;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import jakarta.persistence.EntityManagerFactory;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.test.types.IntegrationTest;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

/**
 * Checks that role capability lists load the endpoints and capabilities collections in batches, not one SQL
 * statement per returned row. Without batching each request below needs more than 150 statements.
 */
@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = "classpath:/sql/capabilities/populate-role-with-many-capabilities.sql")
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql",
})
class CapabilityCollectionFetchIT extends BaseIntegrationTest {

  private static final String ROLE_ID = "1e985e76-e9ca-401c-ad8e-0d121a11111e";
  private static final long MAX_STATEMENTS = 10;

  @Autowired private EntityManagerFactory entityManagerFactory;
  private Statistics statistics;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @BeforeEach
  void setUp() {
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    statistics.setStatisticsEnabled(true);
  }

  @AfterEach
  void tearDown() {
    statistics.setStatisticsEnabled(false);
  }

  @Test
  void findCapabilitiesByRoleId_positive_expanded() throws Exception {
    var result = doGet("/roles/{id}/capabilities?expand=true&limit=5000", ROLE_ID).andReturn();

    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(MAX_STATEMENTS);
    var capabilities = parseResponse(result, Capabilities.class);
    assertThat(capabilities.getTotalRecords()).isEqualTo(300);
    assertThat(capabilities.getCapabilities()).allSatisfy(c -> assertThat(c.getEndpoints()).hasSize(3));
  }

  @Test
  void findCapabilitiesByRoleId_positive_notExpanded() throws Exception {
    var result = doGet("/roles/{id}/capabilities?limit=5000", ROLE_ID).andReturn();

    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(MAX_STATEMENTS);
    var capabilities = parseResponse(result, Capabilities.class);
    assertThat(capabilities.getTotalRecords()).isEqualTo(150);
    assertThat(capabilities.getCapabilities()).allSatisfy(c -> assertThat(c.getEndpoints()).hasSize(3));
  }

  @Test
  void getCapabilitySetsByRoleId_positive() throws Exception {
    var result = doGet("/roles/{id}/capability-sets?limit=5000", ROLE_ID).andReturn();

    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(MAX_STATEMENTS);
    var capabilitySets = parseResponse(result, CapabilitySets.class);
    assertThat(capabilitySets.getTotalRecords()).isEqualTo(150);
    assertThat(capabilitySets.getCapabilitySets()).allSatisfy(cs -> assertThat(cs.getCapabilities()).hasSize(2));
  }
}
