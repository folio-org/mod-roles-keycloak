package org.folio.roles.it;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.valueOf;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.roles.base.BaseIntegrationTest;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@IntegrationTest
class ReferenceDataIT extends BaseIntegrationTest {

  private static final TenantAttributes TENANT_ATTR = new TenantAttributes()
    .addParametersItem(new Parameter()
      .key("loadReference")
      .value("true"));

  @AfterEach
  void afterEach() {
    removeTenant(TENANT_ID);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-ref-data.json")
  void loadReference_true_positive() throws Exception {
    enableTenant(TENANT_ID, TENANT_ATTR);

    mockMvc.perform(get("/roles")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .queryParam("limit", valueOf(MAX_VALUE)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords").value(29));

    mockMvc.perform(get("/policies")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER)
        .queryParam("limit", valueOf(MAX_VALUE)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords").value(1))
      .andExpect(jsonPath("$.policies[0].name").value(equalTo("Business Hours")));
  }

  @Test
  void loadReference_false_positive() throws Exception {
    enableTenant(TENANT_ID);

    mockMvc.perform(get("/roles")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords").value(0));

    mockMvc.perform(get("/policies")
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords").value(0));
  }
}
