package org.folio.roles.it;

import static java.util.UUID.fromString;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.FOO_CREATE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_EDIT_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.FOO_EDIT_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.UI_FOO_CREATE_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.UI_FOO_CREATE_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.UI_FOO_EDIT_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.UI_FOO_EDIT_CAPABILITY_SET;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_RESOURCE;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = "classpath:/sql/truncate-capability-tables.sql", executionPhase = AFTER_TEST_METHOD)
class CapabilitySetIT extends BaseIntegrationTest {

  private static final UUID CREATED_BY_USER_ID = fromString("11111111-1111-4011-1111-0d121a11111e");
  private static final UUID UPDATED_BY_USER_ID = fromString(USER_ID_HEADER);

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void findCapabilitySets_positive() throws Exception {
    mockMvc.perform(get("/capability-sets")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilitySets(
        capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, FOO_CREATE_CAPABILITIES),
        capabilitySet(FOO_EDIT_CAPABILITY_SET, FOO_RESOURCE, EDIT, FOO_EDIT_CAPABILITIES),
        capabilitySet(FOO_MANAGE_CAPABILITY_SET, FOO_RESOURCE, MANAGE, FOO_MANAGE_CAPABILITIES),
        capabilitySet(UI_FOO_CREATE_CAPABILITY_SET, UI_FOO_RESOURCE, CREATE, UI_FOO_CREATE_CAPABILITIES),
        capabilitySet(UI_FOO_EDIT_CAPABILITY_SET, UI_FOO_RESOURCE, EDIT, UI_FOO_EDIT_CAPABILITIES)
      ))));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void findCapabilitySets_positive_offsetAndLimit() throws Exception {
    mockMvc.perform(get("/capability-sets")
        .queryParam("offset", "2")
        .queryParam("limit", "2")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilitySets(5L,
        capabilitySet(FOO_MANAGE_CAPABILITY_SET, FOO_RESOURCE, MANAGE, FOO_MANAGE_CAPABILITIES),
        capabilitySet(UI_FOO_CREATE_CAPABILITY_SET, UI_FOO_RESOURCE, CREATE, UI_FOO_CREATE_CAPABILITIES)
      ))));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void findCapabilitySets_positive_cqlQuery() throws Exception {
    mockMvc.perform(get("/capability-sets")
        .param("query", "name==\"foo_item.create\"")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilitySets(
        capabilitySet(FOO_CREATE_CAPABILITY_SET, FOO_RESOURCE, CREATE, FOO_CREATE_CAPABILITIES)
      ))));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void getCapabilitySetById_positive() throws Exception {
    mockMvc.perform(get("/capability-sets/{id}", FOO_EDIT_CAPABILITY_SET)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(
        capabilitySet(FOO_EDIT_CAPABILITY_SET, FOO_RESOURCE, EDIT, FOO_EDIT_CAPABILITIES)
      )));
  }

  @Test
  void getCapabilitySetById_negative_notFoundError() throws Exception {
    var capabilitySetId = UUID.randomUUID();
    mockMvc.perform(get("/capability-sets/{id}", capabilitySetId)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(capabilitySetNotFoundErrorMessage(capabilitySetId))))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  private static String capabilitySetNotFoundErrorMessage(UUID capabilitySetId) {
    return "Unable to find org.folio.roles.domain.entity.CapabilitySetEntity with id " + capabilitySetId;
  }
}
