package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.CapabilityUtils.PERMISSION_NAME;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySet;
import static org.folio.roles.support.RoleCapabilitySetUtils.roleCapabilitySets;
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

import java.util.List;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.dto.RoleCapabilitySetsRequest;
import org.folio.roles.domain.dto.RolePermissionNamesRequest;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.service.role.RoleService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(RoleCapabilitySetController.class)
@Import({ControllerTestConfiguration.class, RoleCapabilitySetController.class})
class RoleCapabilitySetControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private RoleService roleService;
  @MockBean private CapabilitySetService capabilitySetService;
  @Qualifier("apiRoleCapabilitySetService")
  @MockBean private RoleCapabilitySetService roleCapabilitySetService;

  @Test
  void createRoleCapabilitySets_positive() throws Exception {
    var roleCapabilitySet = roleCapabilitySet();
    var expectedServiceResponse = asSinglePage(roleCapabilitySet);
    when(roleCapabilitySetService.create(ROLE_ID, List.of(CAPABILITY_SET_ID))).thenReturn(expectedServiceResponse);

    var request = new RoleCapabilitySetsRequest().roleId(ROLE_ID).addCapabilitySetIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(post("/roles/capability-sets")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content(asJsonString(request)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilitySets(1, roleCapabilitySet)), true));
  }

  @Test
  void createRoleCapabilitySetsByPermissionName_positive() throws Exception {
    var roleCapabilitySet = roleCapabilitySet();
    var expectedServiceResponse = asSinglePage(roleCapabilitySet);
    var expectedCapabilitySet = capabilitySet();

    when(roleCapabilitySetService.create(ROLE_ID, List.of(CAPABILITY_SET_ID)))
      .thenReturn(expectedServiceResponse);
    when(capabilitySetService.findByPermissionNames(List.of(PERMISSION_NAME)))
      .thenReturn(List.of(expectedCapabilitySet));

    var request = new RolePermissionNamesRequest().roleId(ROLE_ID).addPermissionNamesItem(PERMISSION_NAME);
    mockMvc.perform(post("/roles/capability-sets/permissions")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID)
        .content(asJsonString(request)))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilitySets(1, roleCapabilitySet)), true));
  }

  @Test
  void searchRoleCapabilities_positive() throws Exception {
    var query = "cql.allRecords=1";
    var roleCapabilitySet = roleCapabilitySet();
    when(roleCapabilitySetService.find(query, 100, 20)).thenReturn(asSinglePage(roleCapabilitySet));

    mockMvc.perform(get("/roles/capability-sets")
        .contentType(APPLICATION_JSON)
        .param("query", query)
        .param("limit", "100")
        .param("offset", "20")
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilitySets(1, roleCapabilitySet)), true));
  }

  @Test
  void searchRoleCapabilities_positive_defaultQueryAndPageParameters() throws Exception {
    var roleCapabilitySet = roleCapabilitySet();
    when(roleCapabilitySetService.find(null, 10, 0)).thenReturn(asSinglePage(roleCapabilitySet));

    mockMvc.perform(get("/roles/capability-sets")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(roleCapabilitySets(1, roleCapabilitySet)), true));
  }

  @Test
  void findByRoleId_positive() throws Exception {
    var foundCapabilitySet = capabilitySet();
    when(capabilitySetService.findByRoleId(ROLE_ID, 100, 20)).thenReturn(asSinglePage(foundCapabilitySet));

    mockMvc.perform(get("/roles/{id}/capability-sets", ROLE_ID)
        .param("limit", "100")
        .param("offset", "20")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets(foundCapabilitySet)), true));
  }

  @Test
  void findByRoleId_positive_defaultPageParameters() throws Exception {
    var capabilitySet = capabilitySet();
    when(roleService.getById(ROLE_ID)).thenReturn(role());
    when(capabilitySetService.findByRoleId(ROLE_ID, 10, 0)).thenReturn(asSinglePage(capabilitySet));

    mockMvc.perform(get("/roles/{id}/capability-sets", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets(capabilitySet)), true));
  }

  @Test
  void updateRoleCapabilities_positive() throws Exception {
    var updateRequest = new CapabilitySetsUpdateRequest().addCapabilitySetIdsItem(CAPABILITY_SET_ID);
    mockMvc.perform(put("/roles/{id}/capability-sets", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(updateRequest))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(roleCapabilitySetService).update(ROLE_ID, List.of(CAPABILITY_SET_ID));
  }

  @Test
  void deleteRoleCapabilities_positive() throws Exception {
    mockMvc.perform(delete("/roles/{id}/capability-sets", ROLE_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());

    verify(roleCapabilitySetService).deleteAll(ROLE_ID);
  }
}
