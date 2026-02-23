package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.capabilities;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.roles.service.capability.CapabilityService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(CapabilityController.class)
@Import({ControllerTestConfiguration.class, CapabilityController.class})
class CapabilityControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CapabilityService capabilityService;

  @Test
  void getCapabilitySetById_positive() throws Exception {
    var capability = capability();
    when(capabilityService.get(CAPABILITY_ID)).thenReturn(capability);

    mockMvc.perform(get("/capabilities/{id}", CAPABILITY_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capability), true));
  }

  @Test
  void findCapabilitySets_positive() throws Exception {
    var query = "cql.allRecords = 1";
    var capability = capability();
    when(capabilityService.find(query, 10, 15)).thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/capabilities")
        .queryParam("query", query)
        .queryParam("offset", "15")
        .queryParam("limit", "10")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), true));
  }

  @Test
  void getCapabilitiesByCapabilitySetId_positive() throws Exception {
    var capability = capability();
    when(capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, false, 10, 15))
      .thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/capability-sets/{id}/capabilities", CAPABILITY_SET_ID)
        .queryParam("offset", "15")
        .queryParam("limit", "10")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), JsonCompareMode.STRICT));
    verify(capabilityService).findByCapabilitySetId(CAPABILITY_SET_ID, false, 10, 15);
  }

  @Test
  void getCapabilitiesByCapabilitySetId_positive_includeDummy() throws Exception {
    var capability = capability();
    when(capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, true, 10, 15))
      .thenReturn(asSinglePage(capability));

    mockMvc.perform(get("/capability-sets/{id}/capabilities", CAPABILITY_SET_ID)
        .queryParam("offset", "15")
        .queryParam("limit", "10")
        .queryParam("includeDummy", "true")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilities(capability)), JsonCompareMode.STRICT));
    verify(capabilityService).findByCapabilitySetId(CAPABILITY_SET_ID, true, 10, 15);
  }
}
