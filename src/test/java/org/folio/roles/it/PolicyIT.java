package org.folio.roles.it;

import static java.time.LocalDateTime.of;
import static java.time.Month.JANUARY;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.domain.dto.PolicyType.USER;
import static org.folio.roles.domain.dto.TimePolicy.LogicEnum.POSITIVE;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Policies;
import org.folio.roles.domain.dto.PoliciesRequest;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.domain.dto.TimePolicy;
import org.folio.roles.domain.dto.UserPolicy;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = "classpath:/sql/truncate-policy-tables.sql", executionPhase = AFTER_TEST_METHOD)
class PolicyIT extends BaseIntegrationTest {

  private static final Policy USER_POLICY = buildUserPolicy();
  private static final Policy TIME_POLICY = buildTimePolicy();
  private static final Policy ROLE_POLICY = buildRolePolicy();
  private static final String NOT_EXISTED_POLICY_ID = "1e222e11-e9ca-401c-ad8e-0d121a11111e";

  @Resource private ObjectMapper objectMapper;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @Sql("classpath:/sql/populate-user-based-policy.sql")
  void getPolicy_positive() throws Exception {
    mockMvc
      .perform(get("/policies/{id}", USER_POLICY.getId())
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(USER_POLICY)));
  }

  @Test
  void getPolicy_negative_notFoundError() throws Exception {
    mockMvc
      .perform(get("/policies/{id}", NOT_EXISTED_POLICY_ID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-policies-empty.json")
  void createPolicy_positive() throws Exception {
    var policyToCreate = buildUserPolicy().id(null);
    var policyToCreateAsJson = asJsonString(policyToCreate);
    mockMvc.perform(post("/policies")
        .content(policyToCreateAsJson)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().json(policyToCreateAsJson))
      .andExpect(jsonPath("$.metadata.createdByUserId").value(equalTo(USER_ID_HEADER)))
      .andExpect(jsonPath("$.source").value(equalTo(SourceType.USER.getValue())))
      .andExpect(jsonPath("$.metadata.createdDate").value(notNullValue()))
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(equalTo(USER_ID_HEADER)))
      .andExpect(jsonPath("$.metadata.updatedDate").value(notNullValue()));
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-policies-empty.json")
  @Sql("classpath:/sql/populate-roles-for-policies-batch.sql")
  void createPolicies_positive() throws Exception {
    var response = mockMvc.perform(post("/policies/batch")
        .content(objectMapper.writeValueAsString(new PoliciesRequest()
          .addPoliciesItem(USER_POLICY)
          .addPoliciesItem(ROLE_POLICY)
          .addPoliciesItem(TIME_POLICY)))
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(content().json(objectMapper.writeValueAsString(new Policies()
        .addPoliciesItem(USER_POLICY)
        .addPoliciesItem(ROLE_POLICY)
        .addPoliciesItem(TIME_POLICY))))
      .andReturn();

    var policies = parseResponse(response, Policies.class);
    policies.getPolicies().forEach(policy -> {
      var metadata = policy.getMetadata();
      assertNotNull(metadata.getCreatedByUserId());
      assertNotNull(metadata.getCreatedDate());
      assertNotNull(metadata.getUpdatedByUserId());
      assertNotNull(metadata.getUpdatedDate());
      assertEquals(SourceType.USER, policy.getSource());
    });
  }

  @Test
  void createPolicies_negative_validationError() throws Exception {
    mockMvc
      .perform(post("/policies").header(TENANT, TENANT_ID).contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-policies-with-policies.json")
  @Sql("classpath:/sql/populate-user-based-policy.sql")
  void deletePolicy_positive() throws Exception {
    mockMvc.perform(delete("/policies/{id}", USER_POLICY.getId())
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)).andExpect(status().isNoContent());

    attemptGet("/policies/{id}", USER_POLICY.getId())
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("EntityNotFoundException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  @Test
  void deletePolicy_positive_notFoundInKeycloak() throws Exception {
    mockMvc
      .perform(delete("/policies/{id}", NOT_EXISTED_POLICY_ID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isNoContent());
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-policies-with-policies.json")
  @Sql("classpath:/sql/populate-user-based-policy.sql")
  void updatePolicy_positive() throws Exception {
    var userPolicyForUpdate = buildUserPolicy();
    var newUserId = "07fda6ae-1111-49c1-87be-abeb989c545f";
    var newName = "newName";
    var newDescription = "new-description";
    userPolicyForUpdate.getUserPolicy().setUsers(List.of(fromString(newUserId)));
    userPolicyForUpdate.setName(newName);
    userPolicyForUpdate.setDescription(newDescription);

    mockMvc.perform(put("/policies/{id}", USER_POLICY.getId())
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .content(asJsonString(userPolicyForUpdate))
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());

    var mvcResult = doGet("/policies/{id}", USER_POLICY.getId()).andReturn();
    var updatedPolicy = parseResponse(mvcResult, Policy.class);
    var updatedPolicyMetadata = updatedPolicy.getMetadata();

    assertEquals(USER_ID_HEADER, updatedPolicyMetadata.getUpdatedByUserId().toString());
    assertEquals(newName, updatedPolicy.getName());
    assertEquals(newDescription, updatedPolicy.getDescription());
    assertEquals(newUserId, updatedPolicy.getUserPolicy().getUsers().get(0).toString());
    assertEquals("11111111-1111-4011-1111-0d121a11111e", updatedPolicyMetadata.getCreatedByUserId().toString());
    assertAll(() -> {
      assertNotNull(updatedPolicyMetadata.getCreatedDate());
      assertNotNull(updatedPolicyMetadata.getUpdatedDate());
    });
  }

  @Test
  void updatePolicy_negative_notFound() throws Exception {
    mockMvc
      .perform(put("/policies/{id}", NOT_EXISTED_POLICY_ID)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .content(asJsonString(new Policy()
          .description("updated description")
          .type(USER)
          .name("policy")))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(jsonPath("$.errors[0].type", is("JpaObjectRetrievalFailureException")))
      .andExpect(jsonPath("$.errors[0].code", is("not_found_error")));
  }

  private static Policy buildUserPolicy() {
    var policy = new Policy();
    policy.setId(fromString("1e111e11-1111-401c-ad8e-0d121a11111e"));
    policy.setName("user-based-policy");
    policy.setDescription("hello work");
    policy.setType(USER);
    var userPolicy = new UserPolicy();
    userPolicy.setUsers(List.of(fromString("61893f40-4739-49fc-bf07-daeff3021f90")));
    policy.setUserPolicy(userPolicy);
    return policy;
  }

  private static Policy buildTimePolicy() {
    var policy = new Policy();
    policy.setId(fromString("d86e7054-d51d-4660-8fd0-903c32763928"));
    policy.setName("time-based-policy");
    policy.setDescription("time based policy description");
    policy.setType(TIME);
    var timePolicy = new TimePolicy();
    timePolicy.setRepeat(true);
    timePolicy.setStart(Date.from(of(2023, JANUARY, 25, 0, 0).toInstant(UTC)));
    timePolicy.setExpires(Date.from(of(2023, JANUARY, 28, 0, 0).toInstant(UTC)));
    timePolicy.setDayOfMonthStart(1);
    timePolicy.setDayOfMonthEnd(2);
    timePolicy.setMonthStart(1);
    timePolicy.setMonthEnd(2);
    timePolicy.setHourStart(1);
    timePolicy.setHourEnd(2);
    timePolicy.setMinuteStart(1);
    timePolicy.setMinuteEnd(2);
    timePolicy.setLogic(POSITIVE);
    policy.setTimePolicy(timePolicy);
    return policy;
  }

  private static Policy buildRolePolicy() {
    var policy = new Policy();
    policy.setId(fromString("1e111e11-2222-401c-ad8e-0d121a11111e"));
    policy.setName("test-role-based-policy");
    policy.setDescription("roles based description");
    policy.setType(ROLE);
    var rolePolicy = new RolePolicy();
    rolePolicy.setRoles(List.of(new RolePolicyRole()
      .id(fromString("5eb015a5-7454-4c97-b12c-7fe4162d26a0"))
      .required(false), new RolePolicyRole()
      .id(fromString("392d64d9-ae90-46c7-b338-d20b1744b566"))
      .required(false)));
    policy.setRolePolicy(rolePolicy);
    return policy;
  }
}
