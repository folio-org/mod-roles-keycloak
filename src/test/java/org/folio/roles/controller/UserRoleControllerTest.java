package org.folio.roles.controller;

import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserRoleTestUtils.userRole;
import static org.folio.roles.support.UserRoleTestUtils.userRoles;
import static org.folio.roles.support.UserRoleTestUtils.userRolesRequest;
import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.UUID;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.service.role.UserRoleService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(UserRoleController.class)
@Import({ControllerTestConfiguration.class, UserRoleController.class})
class UserRoleControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private UserRoleService service;

  @Nested
  @DisplayName("deleteUserRoles")
  class DeleteUserRoles {

    @Test
    void positive() throws Exception {
      mockMvc.perform(delete("/roles/users/{userId}", USER_ID))
        .andExpect(status().isNoContent());
    }

    @Test
    void negative_notFound() throws Exception {
      doThrow(EntityNotFoundException.class).when(service).deleteById(any());
      mockMvc.perform(delete("/roles/users/{userId}", USER_ID))
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("getUserRoles")
  class GetUserRoles {

    @Test
    void positive() throws Exception {
      var rolesUser = userRoles(List.of(userRole(ROLE_ID)));

      when(service.findById(USER_ID)).thenReturn(rolesUser);
      mockMvc.perform(get("/roles/users/{userId}", USER_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(asJsonString(rolesUser)));
    }

    @Test
    void negative_notFound() throws Exception {
      doThrow(EntityNotFoundException.class).when(service).findById(any());
      mockMvc.perform(get("/roles/users/{userId}", USER_ID))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(APPLICATION_JSON));
    }
  }

  @Nested
  @DisplayName("assignRolesToUser")
  class AssignRolesToUser {

    @Test
    void positive() throws Exception {
      var request = userRolesRequest();
      var userRoles = userRoles(userRole());
      when(service.create(request)).thenReturn(userRoles);

      mockMvc.perform(post("/roles/users")
          .contentType(APPLICATION_JSON)
          .content(asJsonString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(asJsonString(userRoles)));
    }

    @Test
    void negative_notFound() throws Exception {
      var request = userRolesRequest();
      doThrow(new KeycloakApiException("Failed", new Throwable("message"), 500)).when(service).create(any());

      mockMvc.perform(post("/roles/users")
          .contentType(APPLICATION_JSON)
          .content(asJsonString(request)))
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(status().is5xxServerError());
    }
  }

  @Nested
  @DisplayName("updateUserRoles")
  class UpdateUserRoles {

    @Test
    void positive() throws Exception {
      var request = userRolesRequest();
      mockMvc.perform(put("/roles/users/{userId}", USER_ID)
          .contentType(APPLICATION_JSON)
          .content(asJsonString(request)))
        .andExpect(status().isNoContent());
    }

    @Test
    void negative_validationError() throws Exception {
      var request = userRolesRequest(UUID.randomUUID(), ROLE_ID);
      mockMvc.perform(put("/roles/users/{userId}", USER_ID)
          .contentType(APPLICATION_JSON)
          .content(asJsonString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.total_records", is(1)))
        .andExpect(jsonPath("$.errors[0].message", is("User id in request and in the path must be equal")))
        .andExpect(jsonPath("$.errors[0].type", is("IllegalArgumentException")))
        .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
    }
  }
}
