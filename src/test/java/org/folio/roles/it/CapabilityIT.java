package org.folio.roles.it;

import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.DELETE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.support.CapabilitySetUtils.FOO_EDIT_CAPABILITY_SET;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_MANAGE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.fooItemDeleteEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
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
class CapabilityIT extends BaseIntegrationTest {

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void findCapabilities_positive() throws Exception {
    mockMvc.perform(get("/capabilities")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_CREATE_CAPABILITY, FOO_RESOURCE, CREATE, "foo.item.post", fooItemPostEndpoint()),
        capability(FOO_DELETE_CAPABILITY, FOO_RESOURCE, DELETE, "foo.item.delete", fooItemDeleteEndpoint()),
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint()),
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint()),
        capability(FOO_MANAGE_CAPABILITY, FOO_RESOURCE, MANAGE, "foo.item.all",
          fooItemGetEndpoint(), fooItemPostEndpoint(), fooItemPutEndpoint(), fooItemDeleteEndpoint()),

        capability(UI_FOO_CREATE_CAPABILITY, UI_FOO_RESOURCE, CREATE, "module.foo.item.post"),
        capability(UI_FOO_DELETE_CAPABILITY, UI_FOO_RESOURCE, DELETE, "ui-foo.item.delete"),
        capability(UI_FOO_EDIT_CAPABILITY, UI_FOO_RESOURCE, EDIT, "ui-foo.item.put"),
        capability(UI_FOO_VIEW_CAPABILITY, UI_FOO_RESOURCE, VIEW, "plugin.foo.item.get")
      ))));
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void findCapabilities_positive_offsetAndLimit() throws Exception {
    mockMvc.perform(get("/capabilities")
        .queryParam("offset", "2")
        .queryParam("limit", "3")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilities(9L,
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint()),
        capability(FOO_MANAGE_CAPABILITY, FOO_RESOURCE, MANAGE, "foo.item.all",
          fooItemGetEndpoint(), fooItemPostEndpoint(), fooItemPutEndpoint(), fooItemDeleteEndpoint()),
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint())
      ))));
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void findCapabilities_positive_cqlQuery() throws Exception {
    mockMvc.perform(get("/capabilities")
        .param("query", "name==\"foo_item.create\"")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_CREATE_CAPABILITY, FOO_RESOURCE, CREATE, "foo.item.post", fooItemPostEndpoint())
      ))));
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void findCapabilities_positive_findByPermissionName() throws Exception {
    mockMvc.perform(get("/capabilities")
        .param("query", "permission==(\"foo.item.get\" or \"foo.item.put\")")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint()),
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint())
      ))));
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void getCapabilityById_positive() throws Exception {
    mockMvc.perform(get("/capabilities/{id}", FOO_EDIT_CAPABILITY)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint())
      )));
  }

  @Test
  void getCapabilityById_negative_notFoundError() throws Exception {
    var unknownId = UUID.fromString("2078cf51-dd12-4830-b081-ae849df2793e");
    var errorMessage = "Unable to find org.folio.roles.domain.entity.CapabilityEntity with id " + unknownId;
    mockMvc.perform(get("/capabilities/{id}", unknownId)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(errorMessage)))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void findCapabilitiesByCapabilityId_positive() throws Exception {
    mockMvc.perform(get("/capability-sets/{id}/capabilities", FOO_EDIT_CAPABILITY_SET)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(capabilities(
        capability(FOO_EDIT_CAPABILITY, FOO_RESOURCE, EDIT, "foo.item.put", fooItemPutEndpoint()),
        capability(FOO_VIEW_CAPABILITY, FOO_RESOURCE, VIEW, "foo.item.get", fooItemGetEndpoint())
      ))));
  }

  @Test
  void findCapabilitiesByCapabilityId_negative_capabilitySetIsNotFound() throws Exception {
    var capabilitySetId = UUID.randomUUID();
    var errorMessage = "Unable to find org.folio.roles.domain.entity.CapabilitySetEntity with id " + capabilitySetId;
    mockMvc.perform(get("/capability-sets/{id}/capabilities", capabilitySetId)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(errorMessage)))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }
}
