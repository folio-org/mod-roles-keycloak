package org.folio.roles.controller;

import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySets;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(CapabilitySetController.class)
@Import({ControllerTestConfiguration.class, CapabilitySetController.class})
class CapabilitySetControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private CapabilitySetService capabilitySetService;

  @Test
  void createCapabilitySet_positive() throws Exception {
    var capabilitySet = capabilitySet();
    when(capabilitySetService.create(capabilitySet)).thenReturn(capabilitySet);

    mockMvc.perform(post("/capability-sets")
        .contentType(APPLICATION_JSON)
        .content(asJsonString(capabilitySet))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySet), true));
  }

  @Test
  void createCapabilitySets_positive() throws Exception {
    var capabilitySet = capabilitySet();
    var capabilitySets = capabilitySets(capabilitySet);
    when(capabilitySetService.create(List.of(capabilitySet))).thenReturn(List.of(capabilitySet));

    mockMvc.perform(post("/capability-sets/batch")
        .contentType(APPLICATION_JSON)
        .content(asJsonString(capabilitySets))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isCreated())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(content().json(asJsonString(capabilitySets), true));
  }

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

  @Test
  void updateCapabilitySet_positive() throws Exception {
    var capabilitySet = capabilitySet();
    doNothing().when(capabilitySetService).update(CAPABILITY_SET_ID, capabilitySet);

    mockMvc.perform(put("/capability-sets/{id}", CAPABILITY_SET_ID)
        .contentType(APPLICATION_JSON)
        .content(asJsonString(capabilitySet))
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());
  }

  @Test
  void deleteCapabilitySet_positive() throws Exception {
    doNothing().when(capabilitySetService).delete(CAPABILITY_SET_ID);
    mockMvc.perform(delete("/capability-sets/{id}", CAPABILITY_SET_ID)
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isNoContent());
  }
}
