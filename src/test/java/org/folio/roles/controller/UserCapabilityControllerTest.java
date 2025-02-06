package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.KeycloakUserUtils.keycloakUser;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilities;
import static org.folio.roles.support.UserCapabilityUtils.userCapability;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.UserCapabilitiesRequest;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.UserCapabilityService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(UserCapabilityController.class)
@Import({ControllerTestConfiguration.class, UserCapabilityController.class})
class UserCapabilityControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private CapabilityService capabilityService;
  @MockBean private KeycloakUserService keycloakUserService;
  @MockBean private UserCapabilityService userCapabilityService;

  @Test
  void createUserCapabilities_positive() throws Exception {
    var userCapability = userCapability();
    var expectedServiceResponse = asSinglePage(userCapability);
    when(userCapabilityService.create(USER_ID, List.of(CAPABILITY_SET_ID))).thenReturn(expectedServiceResponse);

    var request = new UserCapabilitiesRequest().userId(USER_ID).addCapabilityIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(post("/users/capabilities")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content(asJsonString(request)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(userCapabilities(1, userCapability)), true));
  }

  @Test
  void findByUserId_positive() throws Exception {
    var foundCapability = capability();
    when(capabilityService.findByUserId(USER_ID, false, 100, 20)).thenReturn(asSinglePage(foundCapability));

    mockMvc.perform(get("/users/{id}/capabilities", USER_ID)
        .param("limit", "100")
        .param("offset", "20")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(foundCapability)), true));
  }

  @Test
  void findByUserId_positive_defaultPageParameters() throws Exception {
    var capability = capability();
    when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
    when(capabilityService.findByUserId(USER_ID, false, 10, 0)).thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/users/{id}/capabilities", USER_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), true));
  }

  @Test
  void findByUserId_positive_expandCapabilities() throws Exception {
    var capability = capability();
    when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
    when(capabilityService.findByUserId(USER_ID, true, 10, 0)).thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/users/{id}/capabilities", USER_ID)
        .queryParam("expand", "true")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), true));
  }

  @Test
  void updateUserCapabilities_positive() throws Exception {
    var updateRequest = new CapabilitiesUpdateRequest().addCapabilityIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(put("/users/{id}/capabilities", USER_ID)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(updateRequest))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(userCapabilityService).update(USER_ID, List.of(CAPABILITY_SET_ID));
  }

  @Test
  void deleteUserCapabilities_positive() throws Exception {
    mockMvc.perform(delete("/users/{id}/capabilities", USER_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(userCapabilityService).deleteAll(USER_ID);
  }
}
