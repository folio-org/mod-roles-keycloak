package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(CapabilitySetController.class)
@Import({ControllerTestConfiguration.class, CapabilitySetController.class})
class CapabilitySetControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CapabilitySetService capabilitySetService;

  @Test
  void getCapabilitySetById_positive() throws Exception {
    var capabilitySet = capabilitySet();
    when(capabilitySetService.get(CAPABILITY_SET_ID)).thenReturn(capabilitySet);

    mockMvc.perform(get("/capability-sets/{id}", CAPABILITY_SET_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySet), true));
  }

  @Test
  void findCapabilitySets_positive() throws Exception {
    var query = "cql.allRecords = 1";
    var capabilitySet = capabilitySet();
    when(capabilitySetService.find(query, 10, 15)).thenReturn(asSinglePage(capabilitySet));

    mockMvc.perform(get("/capability-sets")
        .queryParam("query", query)
        .queryParam("offset", "15")
        .queryParam("limit", "10")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets(capabilitySet)), true));
  }
}
