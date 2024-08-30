package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.KeycloakUserUtils.KEYCLOAK_USER_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.keycloakTimePolicy;
import static org.folio.roles.support.PolicyUtils.keycloakUserPolicy;
import static org.folio.roles.support.PolicyUtils.timePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakPolicyMapper;
import org.folio.roles.mapper.KeycloakPolicyMapper.PolicyMapperContext;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PolicyResource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakPolicyServiceTest {

  @InjectMocks private KeycloakPolicyService keycloakPolicyService;

  @Mock private PolicyResource policyResource;
  @Mock private KeycloakUserService keycloakUserService;
  @Mock private KeycloakPolicyMapper keycloakPolicyMapper;
  @Mock private KeycloakAuthorizationClientProvider authClientProvider;
  @Mock(answer = RETURNS_DEEP_STUBS) private AuthorizationResource authorizationResource;

  @AfterEach
  void afterEach() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    void positive() {
      var expectedPolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString()).toRepresentation()).thenReturn(keycloakPolicy);
      when(keycloakPolicyMapper.toPolicy(keycloakPolicy)).thenReturn(expectedPolicy);

      var policy = keycloakPolicyService.getById(POLICY_ID);

      assertThat(policy).isEqualTo(expectedPolicy);

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void negative_throwsNotFoundException() {
      var exception = new NotFoundException();

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString()).toRepresentation()).thenThrow(exception);

      assertThatThrownBy(() -> keycloakPolicyService.getById(POLICY_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Failed to find policy: id = %s", POLICY_ID);

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void negative_throwsClientErrorException() {
      var response = new ServerResponse(null, 500, new Headers<>());
      var exception = new InternalServerErrorException(response);

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString()).toRepresentation()).thenThrow(exception);

      assertThatThrownBy(() -> keycloakPolicyService.getById(POLICY_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to find policy: id = %s", POLICY_ID);

      verify(authorizationResource, atLeastOnce()).policies();
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var timePolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();
      var response = mock(Response.class);

      when(response.getStatusInfo()).thenReturn(Status.CREATED);
      when(keycloakPolicyMapper.toKeycloakPolicy(timePolicy, PolicyMapperContext.empty())).thenReturn(keycloakPolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().create(keycloakPolicy)).thenReturn(response);

      keycloakPolicyService.create(timePolicy);

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void positive_policyExistsByName() {
      var timePolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();
      var response = mock(Response.class);

      when(response.getStatusInfo()).thenReturn(Status.CONFLICT);
      when(keycloakPolicyMapper.toKeycloakPolicy(timePolicy, PolicyMapperContext.empty())).thenReturn(keycloakPolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().create(keycloakPolicy)).thenReturn(response);

      keycloakPolicyService.create(timePolicy);

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void positive_userPolicy() {
      var userPolicy = userPolicy();
      var policyMapperContext = new PolicyMapperContext().keycloakUserIds(List.of(KEYCLOAK_USER_ID));
      var keycloakPolicy = keycloakUserPolicy();
      var response = mock(Response.class);

      when(response.getStatusInfo()).thenReturn(Status.CONFLICT);
      when(keycloakUserService.findKeycloakIdByUserId(USER_ID)).thenReturn(KEYCLOAK_USER_ID);
      when(keycloakPolicyMapper.toKeycloakPolicy(userPolicy, policyMapperContext)).thenReturn(keycloakPolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().create(keycloakPolicy)).thenReturn(response);

      keycloakPolicyService.create(userPolicy);

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void positive_notAuthorizedException() {
      var timePolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();
      var response = mock(Response.class);

      when(response.getStatusInfo()).thenReturn(Status.UNAUTHORIZED);
      when(keycloakPolicyMapper.toKeycloakPolicy(timePolicy, PolicyMapperContext.empty())).thenReturn(keycloakPolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().create(keycloakPolicy)).thenReturn(response);

      assertThatThrownBy(() -> keycloakPolicyService.create(timePolicy))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Error during policy creation in Keycloak. Details: id = %s, status = %s, message = %s",
          POLICY_ID, 401, "Unauthorized");

      verify(authorizationResource, atLeastOnce()).policies();
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_returnsAllNullParameters() {
      var userPolicyId = UUID.randomUUID();
      var timePolicyId = UUID.randomUUID();
      var userPolicy = userPolicy(KEYCLOAK_USER_ID).id(userPolicyId).source(null);
      var timePolicy = timePolicy().id(timePolicyId).source(null);
      var keycloakUserPolicy = keycloakUserPolicy(userPolicyId);
      var keycloakTimePolicy = keycloakTimePolicy(timePolicyId);
      var query = "test";

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policies(null, query, null, null, null, false, null, null, 5, 10))
        .thenReturn(List.of(keycloakTimePolicy, keycloakUserPolicy));
      when(keycloakPolicyMapper.toPolicy(keycloakTimePolicy)).thenReturn(timePolicy);
      when(keycloakPolicyMapper.toPolicy(keycloakUserPolicy)).thenReturn(userPolicy);

      var result = keycloakPolicyService.find(query, 10, 5);

      assertThat(result).containsExactly(timePolicy, userPolicy);
      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void positive_emptyResults() {
      var query = "test";

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policies(null, query, null, null, null, false, null, null, 5, 10))
        .thenReturn(emptyList());

      var result = keycloakPolicyService.find(query, 10, 5);

      assertThat(result).isEmpty();
      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void positive_webApplicationException() {
      var query = "test";

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policies(null, query, null, null, null, false, null, null, 5, 10))
        .thenThrow(new NotAuthorizedException("Unauthorized"));

      assertThatThrownBy(() -> keycloakPolicyService.find(query, 10, 5))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to search policies");

      verify(authorizationResource, atLeastOnce()).policies();
    }
  }

  @Nested
  @DisplayName("updateById")
  class UpdateById {

    @Test
    void positive() {
      var policy = timePolicy();
      var keycloakTimePolicy = keycloakTimePolicy();

      when(keycloakPolicyMapper.toKeycloakPolicy(policy, PolicyMapperContext.empty())).thenReturn(keycloakTimePolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString())).thenReturn(policyResource);
      doNothing().when(policyResource).update(keycloakTimePolicy);

      keycloakPolicyService.update(policy);
      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void negative_internalServerErrorException() {
      var policy = timePolicy();
      var keycloakTimePolicy = keycloakTimePolicy();
      var exception = new InternalServerErrorException(new ServerResponse(null, 500, new Headers<>()));

      when(keycloakPolicyMapper.toKeycloakPolicy(policy, PolicyMapperContext.empty())).thenReturn(keycloakTimePolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString())).thenReturn(policyResource);
      doThrow(exception).when(policyResource).update(keycloakTimePolicy);

      assertThatThrownBy(() -> keycloakPolicyService.update(policy))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to update policy")
        .satisfies(err -> assertThat(((KeycloakApiException) err).getStatus()).isEqualTo(NOT_FOUND));

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void negative_unauthorizedException() {
      var policy = timePolicy();
      var keycloakTimePolicy = keycloakTimePolicy();
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));

      when(keycloakPolicyMapper.toKeycloakPolicy(policy, PolicyMapperContext.empty())).thenReturn(keycloakTimePolicy);
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString())).thenReturn(policyResource);
      doThrow(exception).when(policyResource).update(keycloakTimePolicy);

      assertThatThrownBy(() -> keycloakPolicyService.update(policy))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to update policy")
        .satisfies(err -> assertThat(((KeycloakApiException) err).getStatus()).isEqualTo(UNAUTHORIZED));

      verify(authorizationResource, atLeastOnce()).policies();
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString())).thenReturn(policyResource);
      doNothing().when(policyResource).remove();

      keycloakPolicyService.deleteById(POLICY_ID);

      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void negative_notFoundException() {
      var exception = new NotFoundException(new ServerResponse(null, 404, new Headers<>()));

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString())).thenReturn(policyResource);
      doThrow(exception).when(policyResource).remove();

      assertThatThrownBy(() -> keycloakPolicyService.deleteById(POLICY_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Failed to find policy: id = %s", POLICY_ID);
      verify(authorizationResource, atLeastOnce()).policies();
    }

    @Test
    void negative_notAuthorizedException() {
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));

      when(authClientProvider.getAuthorizationClient()).thenReturn(authorizationResource);
      when(authorizationResource.policies().policy(POLICY_ID.toString())).thenReturn(policyResource);
      doThrow(exception).when(policyResource).remove();

      assertThatThrownBy(() -> keycloakPolicyService.deleteById(POLICY_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to delete policy")
        .satisfies(err -> assertThat(((KeycloakApiException) err).getStatus()).isEqualTo(UNAUTHORIZED));
      verify(authorizationResource, atLeastOnce()).policies();
    }
  }
}
