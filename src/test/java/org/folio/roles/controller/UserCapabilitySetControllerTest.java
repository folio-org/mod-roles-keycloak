package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.KeycloakUserUtils.keycloakUser;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySet;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySets;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.UserCapabilitySetNames;
import org.folio.roles.domain.dto.UserCapabilitySetsQueryRequest;
import org.folio.roles.domain.dto.UserCapabilitySetsQueryResult;
import org.folio.roles.domain.dto.UserCapabilitySetsRequest;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.UserCapabilitySetQueryService;
import org.folio.roles.service.capability.UserCapabilitySetService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(UserCapabilitySetController.class)
@Import({ControllerTestConfiguration.class, UserCapabilitySetController.class})
class UserCapabilitySetControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private KeycloakUserService keycloakUserService;
  @MockitoBean private CapabilitySetService capabilitySetService;
  @MockitoBean private UserCapabilitySetQueryService userCapabilitySetQueryService;
  @MockitoBean private UserCapabilitySetService userCapabilitySetService;

  @Test
  void createUserCapabilities_positive() throws Exception {
    var userCapabilitySet = userCapabilitySet();
    var expectedServiceResponse = asSinglePage(userCapabilitySet);
    when(userCapabilitySetService.create(USER_ID, List.of(CAPABILITY_SET_ID))).thenReturn(expectedServiceResponse);

    var request = new UserCapabilitySetsRequest().userId(USER_ID).addCapabilitySetIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(post("/users/capability-sets")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content(asJsonString(request)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(userCapabilitySets(1, userCapabilitySet)), JsonCompareMode.STRICT));
  }

  @Test
  void searchUserCapabilities_positive() throws Exception {
    var query = "cql.allRecords=1";
    var userCapabilitySet = userCapabilitySet();
    when(userCapabilitySetService.find(query, 100, 20)).thenReturn(asSinglePage(userCapabilitySet));

    mockMvc.perform(get("/users/capability-sets")
        .contentType(APPLICATION_JSON)
        .param("query", query)
        .param("limit", "100")
        .param("offset", "20")
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(userCapabilitySets(1, userCapabilitySet)), JsonCompareMode.STRICT));
  }

  @Test
  void searchUserCapabilities_positive_defaultQueryAndPageParameters() throws Exception {
    var userCapabilitySet = userCapabilitySet();
    when(userCapabilitySetService.find(null, 10, 0)).thenReturn(asSinglePage(userCapabilitySet));

    mockMvc.perform(get("/users/capability-sets")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(userCapabilitySets(1, userCapabilitySet)), JsonCompareMode.STRICT));
  }

  @Test
  void queryUserCapabilitySets_positive() throws Exception {
    var request = new UserCapabilitySetsQueryRequest()
      .userIds(List.of(USER_ID));
    var response = new UserCapabilitySetsQueryResult()
      .addUserCapabilitySetsItem(new UserCapabilitySetNames()
        .userId(USER_ID).capabilitySetNames(List.of("foo_item.view")));
    when(userCapabilitySetQueryService.query(request)).thenReturn(response);

    mockMvc.perform(post("/users/capability-sets/query")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content("{\"userIds\":[\"" + USER_ID + "\"]}"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(response), JsonCompareMode.STRICT));
  }

  @Test
  void queryUserCapabilitySets_negative_moreThan500UserIdsReturnsBadRequest() throws Exception {
    var userIds = new ArrayList<UUID>();
    for (var index = 0; index < 501; index++) {
      userIds.add(UUID.randomUUID());
    }

    mockMvc.perform(post("/users/capability-sets/query")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content("{\"userIds\":" + asJsonString(userIds) + "}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
      .andExpect(jsonPath("$.errors[0].parameters[0].key", is("userIds")));
  }

  @Test
  void queryUserCapabilitySets_negative_emptyUserIdsReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/users/capability-sets/query")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content("{\"userIds\":[]}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void queryUserCapabilitySets_positive_capabilityNamesEmpty() throws Exception {
    mockMvc.perform(post("/users/capability-sets/query")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content("""
          {
            "userIds": ["10000000-0000-0000-0000-000000000001"],
            "capabilitySetNames": []
          }
          """))
      .andExpect(status().isOk());
  }

  @Test
  void findByUserId_positive() throws Exception {
    var foundCapabilitySet = capabilitySet();
    when(capabilitySetService.findByUserId(USER_ID, 100, 20)).thenReturn(asSinglePage(foundCapabilitySet));

    mockMvc.perform(get("/users/{id}/capability-sets", USER_ID)
        .param("limit", "100")
        .param("offset", "20")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets(foundCapabilitySet)), JsonCompareMode.STRICT));
  }

  @Test
  void findByUserId_positive_defaultPageParameters() throws Exception {
    var capabilitySet = capabilitySet();
    when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
    when(capabilitySetService.findByUserId(USER_ID, 10, 0)).thenReturn(asSinglePage(capabilitySet));

    mockMvc.perform(get("/users/{id}/capability-sets", USER_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets(capabilitySet)), JsonCompareMode.STRICT));
  }

  @Test
  void updateUserCapabilities_positive() throws Exception {
    var updateRequest = new CapabilitySetsUpdateRequest().addCapabilitySetIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(put("/users/{id}/capability-sets", USER_ID)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(updateRequest))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(userCapabilitySetService).update(USER_ID, List.of(CAPABILITY_SET_ID));
  }

  @Test
  void deleteUserCapabilities_positive() throws Exception {
    mockMvc.perform(delete("/users/{id}/capability-sets", USER_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(userCapabilitySetService).deleteAll(USER_ID);
  }
}
