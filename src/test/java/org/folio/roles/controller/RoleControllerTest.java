package org.folio.roles.controller;

import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.RoleUtils.role2;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.dto.RolesRequest;
import org.folio.roles.service.role.RoleService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(RoleController.class)
@Import({ControllerTestConfiguration.class, RoleController.class})
class RoleControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private RoleService roleService;

  @Nested
  @DisplayName("createRole")
  class CreateRole {

    @Test
    void positive() throws Exception {
      var role = new Role().name("Cataloger basic-view.v1~2").description("test");
      when(roleService.create(any(Role.class))).thenReturn(role);

      mockMvc.perform(post("/roles")
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(role)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(asJsonString(role)));
    }

    @Test
    void negative_roleNameContainsForbiddenCharacter() throws Exception {
      var invalidName = "Circulation/Administrator";

      mockMvc.perform(post("/roles")
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(new Role().name(invalidName).description("test"))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
        .andExpect(jsonPath("$.errors[0].parameters[0].key", is("name")))
        .andExpect(jsonPath("$.errors[0].parameters[0].value", is(invalidName)));
    }

    @Test
    void negative_nullName() throws Exception {
      mockMvc.perform(post("/roles")
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content("{\"description\": \"test\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
    }

    @Test
    void negative_emptyName() throws Exception {
      mockMvc.perform(post("/roles")
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(new Role().name("").description("test"))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
    }
  }

  @Nested
  @DisplayName("createRoles")
  class CreateRoles {

    @Test
    void positive() throws Exception {
      when(roleService.create(anyList())).thenReturn(new Roles().roles(List.of(role(), role2())));

      mockMvc.perform(post("/roles/batch")
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(new RolesRequest().addRolesItem(role()).addRolesItem(role2()))))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    void negative_roleNameContainsForbiddenCharacter() throws Exception {
      var invalidRole = new Role().name("Circulation/Administrator").description("test");

      mockMvc.perform(post("/roles/batch")
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(new RolesRequest().addRolesItem(role()).addRolesItem(invalidRole))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
    }
  }

  @Nested
  @DisplayName("updateRole")
  class UpdateRole {

    @Test
    void positive() throws Exception {
      var roleForUpdate = new Role().id(ROLE_ID).name("updated name").description("updated description");

      mockMvc.perform(put("/roles/{id}", ROLE_ID)
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(roleForUpdate)))
        .andExpect(status().isNoContent());
    }

    @Test
    void negative_roleNameContainsForbiddenCharacter() throws Exception {
      var invalidName = "Circulation/Administrator";
      var roleForUpdate = new Role().id(ROLE_ID).name(invalidName).description("test");

      mockMvc.perform(put("/roles/{id}", ROLE_ID)
          .contentType(APPLICATION_JSON)
          .header(TENANT, TENANT_ID)
          .content(asJsonString(roleForUpdate)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
        .andExpect(jsonPath("$.errors[0].parameters[0].key", is("name")));
    }
  }

  @Nested
  @DisplayName("getRole")
  class GetRole {

    @Test
    void positive() throws Exception {
      var role = role();
      when(roleService.getById(ROLE_ID)).thenReturn(role);

      mockMvc.perform(get("/roles/{id}", ROLE_ID)
          .header(TENANT, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(asJsonString(role)));
    }

    @Test
    void negative_notFound() throws Exception {
      doThrow(EntityNotFoundException.class).when(roleService).getById(ROLE_ID);

      mockMvc.perform(get("/roles/{id}", ROLE_ID)
          .header(TENANT, TENANT_ID))
        .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("findRoles")
  class FindRoles {

    @Test
    void positive() throws Exception {
      when(roleService.search(anyString(), anyInt(), anyInt())).thenReturn(new Roles().roles(List.of(role())));

      mockMvc.perform(get("/roles")
          .header(TENANT, TENANT_ID)
          .queryParam("query", "cql.allRecords=1")
          .queryParam("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON));
    }
  }

  @Nested
  @DisplayName("deleteRole")
  class DeleteRole {

    @Test
    void positive() throws Exception {
      mockMvc.perform(delete("/roles/{id}", ROLE_ID)
          .header(TENANT, TENANT_ID))
        .andExpect(status().isNoContent());
    }

    @Test
    void negative_notFound() throws Exception {
      doThrow(EntityNotFoundException.class).when(roleService).deleteById(ROLE_ID);

      mockMvc.perform(delete("/roles/{id}", ROLE_ID)
          .header(TENANT, TENANT_ID))
        .andExpect(status().isNotFound());
    }
  }
}
