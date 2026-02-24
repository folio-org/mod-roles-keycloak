package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_NAME;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_NAME;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapabilities;
import static org.folio.roles.support.RoleCapabilityUtils.roleCapability;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.TestConstants.TENANT_ID;
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

import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.role.RoleService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(RoleCapabilityController.class)
@Import({ControllerTestConfiguration.class, RoleCapabilityController.class})
class RoleCapabilityControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private RoleService roleService;
  @MockitoBean private CapabilityService capabilityService;
  @Qualifier("apiRoleCapabilityService")
  @MockitoBean private RoleCapabilityService roleCapabilityService;

  @Test
  void createRoleCapabilities_positive() throws Exception {
    var roleCapability = roleCapability();
    var roleCapabilities = asSinglePage(roleCapability);
    var expectedRequest = new RoleCapabilitiesRequest().roleId(ROLE_ID).addCapabilityIdsItem(CAPABILITY_ID);

    when(roleCapabilityService.create(expectedRequest, false)).thenReturn(roleCapabilities);

    var request = new RoleCapabilitiesRequest().roleId(ROLE_ID).addCapabilityIdsItem(CAPABILITY_ID);
    mockMvc.perform(post("/roles/capabilities")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content(asJsonString(request)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilities(1, roleCapability)), JsonCompareMode.STRICT));
  }

  @Test
  void createRoleCapabilitiesByNames_positive() throws Exception {
    var roleCapability = roleCapability();
    var roleCapabilities = asSinglePage(roleCapability);
    var expectedRequest = new RoleCapabilitiesRequest().roleId(ROLE_ID).addCapabilityNamesItem(CAPABILITY_NAME);

    when(roleCapabilityService.create(expectedRequest, false)).thenReturn(roleCapabilities);

    var request = new RoleCapabilitiesRequest().roleId(ROLE_ID).addCapabilityNamesItem(CAPABILITY_NAME);
    mockMvc.perform(post("/roles/capabilities")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content(asJsonString(request)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilities(1, roleCapability)), JsonCompareMode.STRICT));
  }

  @Test
  void searchRoleCapabilities_positive() throws Exception {
    var query = "cql.allRecords=1";
    var roleCapability = roleCapability();
    when(roleCapabilityService.find(query, 100, 20)).thenReturn(asSinglePage(roleCapability));

    mockMvc.perform(get("/roles/capabilities")
        .contentType(APPLICATION_JSON)
        .param("query", query)
        .param("limit", "100")
        .param("offset", "20")
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilities(1, roleCapability)), JsonCompareMode.STRICT));
  }

  @Test
  void searchRoleCapabilities_positive_defaultQueryAndPageParameters() throws Exception {
    var roleCapability = roleCapability();
    when(roleCapabilityService.find(null, 10, 0)).thenReturn(asSinglePage(roleCapability));

    mockMvc.perform(get("/roles/capabilities")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilities(1, roleCapability)), JsonCompareMode.STRICT));
  }

  @Test
  void findByRoleId_positive() throws Exception {
    var foundCapability = capability();
    when(capabilityService.findByRoleId(ROLE_ID, false, false, 100, 20))
      .thenReturn(asSinglePage(foundCapability));

    mockMvc.perform(get("/roles/{id}/capabilities", ROLE_ID)
        .param("limit", "100")
        .param("offset", "20")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(foundCapability)), JsonCompareMode.STRICT));
  }

  @Test
  void findByRoleId_positive_defaultPageParameters() throws Exception {
    var capability = capability();
    when(roleService.getById(ROLE_ID)).thenReturn(role());
    when(capabilityService.findByRoleId(ROLE_ID, false, false, 10, 0)).thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/roles/{id}/capabilities", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), JsonCompareMode.STRICT));
    verify(capabilityService).findByRoleId(ROLE_ID, false, false, 10, 0);
  }

  @Test
  void findByRoleId_positive_expandCapabilities() throws Exception {
    var capability = capability();
    when(roleService.getById(ROLE_ID)).thenReturn(role());
    when(capabilityService.findByRoleId(ROLE_ID, true, false, 10, 0)).thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/roles/{id}/capabilities", ROLE_ID)
        .queryParam("expand", "true")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), JsonCompareMode.STRICT));
    verify(capabilityService).findByRoleId(ROLE_ID, true, false, 10, 0);
  }

  @Test
  void findByRoleId_positive_includeDummyCapabilities() throws Exception {
    var capability = capability();
    when(roleService.getById(ROLE_ID)).thenReturn(role());
    when(capabilityService.findByRoleId(ROLE_ID, false, true, 10, 0)).thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/roles/{id}/capabilities", ROLE_ID)
        .queryParam("includeDummy", "true")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), JsonCompareMode.STRICT));
    verify(capabilityService).findByRoleId(ROLE_ID, false, true, 10, 0);
  }

  @Test
  void updateRoleCapabilities_positive() throws Exception {
    var updateRequest = new CapabilitiesUpdateRequest().addCapabilityIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(put("/roles/{id}/capabilities", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(updateRequest))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(roleCapabilityService).update(ROLE_ID, updateRequest);
  }

  @Test
  void updateRoleCapabilitiesByNames_positive() throws Exception {
    var updateRequest = new CapabilitiesUpdateRequest().addCapabilityNamesItem(CAPABILITY_SET_NAME);
    mockMvc.perform(put("/roles/{id}/capabilities", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(updateRequest))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(roleCapabilityService).update(ROLE_ID, updateRequest);
  }

  @Test
  void deleteRoleCapabilities_positive() throws Exception {
    mockMvc.perform(delete("/roles/{id}/capabilities", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(roleCapabilityService).deleteAll(ROLE_ID);
  }
}
