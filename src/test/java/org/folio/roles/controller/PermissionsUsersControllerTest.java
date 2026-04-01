package org.folio.roles.controller;

import static org.folio.roles.support.CapabilityUtils.PERMISSION_NAME;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.List;
import org.folio.roles.domain.dto.PermissionsUser;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(PermissionsUsersController.class)
@Import({ControllerTestConfiguration.class, PermissionsUsersController.class})
class PermissionsUsersControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CapabilityService capabilityService;

  @Test
  void getPermissionsUser_positive() throws Exception {
    var foundPermissions = List.of(PERMISSION_NAME);
    when(capabilityService.getUserPermissions(USER_ID, false, null)).thenReturn(foundPermissions);

    mockMvc.perform(get("/permissions/users/{id}", USER_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(
        new PermissionsUser().userId(USER_ID).permissions(foundPermissions))
      ));
  }

  @Test
  void getPermissionsUser_positive_onlyVisibleIsTrue() throws Exception {
    var foundPermissions = List.of(PERMISSION_NAME);
    when(capabilityService.getUserPermissions(USER_ID, true, null)).thenReturn(foundPermissions);

    mockMvc.perform(get("/permissions/users/{id}", USER_ID)
      .param("onlyVisible", "true")
      .contentType(APPLICATION_JSON)
      .header(TENANT, TENANT_ID))
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(
        new PermissionsUser().userId(USER_ID).permissions(foundPermissions))
      ));
  }
}
