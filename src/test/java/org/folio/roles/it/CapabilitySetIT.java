package org.folio.roles.it;

import static java.util.Collections.emptyList;
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
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_RESOURCE;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void createCapabilitySet_positive() throws Exception {
    var capabilitySet = capabilitySet(List.of(FOO_EDIT_CAPABILITY, FOO_DELETE_CAPABILITY));
    mockMvc.perform(post("/capability-sets")
        .content(asJsonString(capabilitySet))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().json(asJsonString(capabilitySet)))
      .andExpect(jsonPath("$.id").value(notNullValue()))
      .andExpect(jsonPath("$.metadata.createdBy").value(equalTo(USER_ID_HEADER)))
      .andExpect(jsonPath("$.metadata.createdDate").value(notNullValue()))
      .andExpect(jsonPath("$.metadata.modifiedBy").doesNotExist())
      .andExpect(jsonPath("$.metadata.modifiedDate").doesNotExist());
  }

  @Test
  void createCapabilitySets_negative_capabilityIdsAreNotFound() throws Exception {
    var capabilityIds = List.of(FOO_EDIT_CAPABILITY, FOO_DELETE_CAPABILITY);
    var capabilitySet = capabilitySet(capabilityIds);
    mockMvc.perform(post("/capability-sets")
        .content(asJsonString(capabilitySet))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.total_records").value(1))
      .andExpect(jsonPath("$.errors[0].message").value("Capabilities not found by ids: " + capabilityIds))
      .andExpect(jsonPath("$.errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("$.errors[0].code").value("not_found_error"));
  }

  @Test
  @Sql("classpath:/sql/capabilities/populate-capabilities.sql")
  void createCapabilitySets_positive() throws Exception {
    var capabilitySet = capabilitySet(List.of(FOO_EDIT_CAPABILITY, FOO_DELETE_CAPABILITY));
    mockMvc.perform(post("/capability-sets/batch")
        .content(asJsonString(capabilitySets(capabilitySet)))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().json(asJsonString(capabilitySets(capabilitySet))))
      .andExpect(jsonPath("$.capabilitySets[0].metadata.createdBy", is(USER_ID_HEADER)))
      .andExpect(jsonPath("$.capabilitySets[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.capabilitySets[0].metadata.modifiedBy").doesNotExist())
      .andExpect(jsonPath("$.capabilitySets[0].metadata.modifiedDate").doesNotExist());
  }

  @Test
  void createCapabilitySets_negative_emptyCapabilityIds() throws Exception {
    var capabilitySet = capabilitySet(emptyList());
    mockMvc.perform(post("/capability-sets/batch")
        .content(asJsonString(capabilitySets(capabilitySet)))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records").value(1))
      .andExpect(jsonPath("$.errors[0].message").value("size must be between 1 and 2147483647"))
      .andExpect(jsonPath("$.errors[0].type").value("MethodArgumentNotValidException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"))
      .andExpect(jsonPath("$.errors[0].parameters[0].key").value("capabilitySets[0].capabilities"))
      .andExpect(jsonPath("$.errors[0].parameters[0].value").value("[]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void creatCapabilitySets_negative_duplicateName() throws Exception {
    var capabilitySet = capabilitySet(null, FOO_RESOURCE, CREATE, List.of(FOO_CREATE_CAPABILITY));
    mockMvc.perform(post("/capability-sets/batch")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(capabilitySets(capabilitySet)))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets())));
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void creatCapabilitySet_negative_duplicateName() throws Exception {
    var capabilitySet = capabilitySet(null, FOO_RESOURCE, CREATE, List.of(FOO_CREATE_CAPABILITY));
    mockMvc.perform(post("/capability-sets")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(capabilitySet))
        .contentType(APPLICATION_JSON))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records").value(1))
      .andExpect(jsonPath("$.errors[0].message").value("Capability set name is already taken"))
      .andExpect(jsonPath("$.errors[0].type").value("RequestValidationException"))
      .andExpect(jsonPath("$.errors[0].code").value("validation_error"))
      .andExpect(jsonPath("$.errors[0].parameters[0].key").value("name"))
      .andExpect(jsonPath("$.errors[0].parameters[0].value").value("foo_item.create"));
  }

  @Test
  void createCapabilitySets_negative_validationError() throws Exception {
    mockMvc.perform(post("/capability-sets/batch")
        .header(TENANT, TENANT_ID)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void deleteCapabilitySetById_positive() throws Exception {
    mockMvc.perform(delete("/capability-sets/{id}", FOO_EDIT_CAPABILITY_SET)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());

    attemptGet("/capability-sets/{id}", FOO_EDIT_CAPABILITY_SET)
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].message", is(capabilitySetNotFoundErrorMessage(FOO_EDIT_CAPABILITY_SET))))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  void deleteCapabilitySetById_negative_notFoundError() throws Exception {
    mockMvc.perform(delete("/capability-sets/{id}", UUID.randomUUID())
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());
  }

  @Test
  @Sql(scripts = {
    "classpath:/sql/capabilities/populate-capabilities.sql",
    "classpath:/sql/capability-sets/populate-capability-sets.sql",
  })
  void updateCapabilityById_positive() throws Exception {
    var capabilitySet = capabilitySet(UI_FOO_EDIT_CAPABILITY_SET, UI_FOO_RESOURCE, MANAGE, List.of(
      UI_FOO_VIEW_CAPABILITY, UI_FOO_CREATE_CAPABILITY, UI_FOO_EDIT_CAPABILITY, UI_FOO_DELETE_CAPABILITY));

    mockMvc.perform(put("/capability-sets/{id}", UI_FOO_EDIT_CAPABILITY_SET)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, UPDATED_BY_USER_ID)
        .content(asJsonString(capabilitySet))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    doGet("/capability-sets/{id}", UI_FOO_EDIT_CAPABILITY_SET)
      .andExpect(content().json(asJsonString(capabilitySet)))
      .andExpect(jsonPath("$.metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("$.metadata.modifiedDate", notNullValue()))
      .andExpect(jsonPath("$.metadata.createdBy", is(CREATED_BY_USER_ID.toString())))
      .andExpect(jsonPath("$.metadata.modifiedBy", is(UPDATED_BY_USER_ID.toString())));
  }

  @Test
  void updateCapabilityById_negative_notFoundError() throws Exception {
    var capabilityId = UUID.randomUUID();
    var capability = capabilitySet(capabilityId);
    mockMvc.perform(put("/capability-sets/{id}", capabilityId)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(capability))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].message", is(capabilitySetNotFoundErrorMessage(capabilityId))))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  private static String capabilitySetNotFoundErrorMessage(UUID capabilitySetId) {
    return "Unable to find org.folio.roles.domain.entity.CapabilitySetEntity with id " + capabilitySetId;
  }
}
