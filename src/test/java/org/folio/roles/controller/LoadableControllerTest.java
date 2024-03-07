package org.folio.roles.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.LoadableRoleUtils.loadableRole;
import static org.folio.roles.support.LoadableRoleUtils.loadableRoles;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.parseResponse;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(LoadableRoleController.class)
@Import({ControllerTestConfiguration.class, LoadableRoleController.class})
class LoadableControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private LoadableRoleService service;

  @Test
  void findLoadableRoles_positive() throws Exception {
    var query = "cql.allRecords = 1";
    var loadableRoles = loadableRoles(loadableRole());
    when(service.find(query, 10, 15)).thenReturn(loadableRoles);

    var mvcResult = mockMvc.perform(get("/loadable-roles")
        .queryParam("query", query)
        .queryParam("offset", "15")
        .queryParam("limit", "10")
        .contentType(APPLICATION_JSON)
        .header(TENANT, TENANT_ID))
      .andExpect(status().isOk())
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn();

    var actual = parseResponse(mvcResult, LoadableRoles.class);
    assertThat(actual).isEqualTo(loadableRoles);
  }
}
