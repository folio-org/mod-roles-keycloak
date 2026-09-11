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
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakPolicyMapper;
import org.folio.roles.mapper.KeycloakPolicyMapper.PolicyMapperContext;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.ClientRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakPolicyServiceTest {

  private static final String CLIENT_UUID = UUID.randomUUID().toString();

  @InjectMocks private KeycloakPolicyService keycloakPolicyService;

  @Mock private KeycloakUserService userService;
  @Mock private KeycloakPolicyMapper keycloakPolicyMapper;
  @Mock private KeycloakAdminClient keycloakAdminClient;
  @Mock private KeycloakClientService keycloakClientService;
  @Mock private FolioExecutionContext context;

  @BeforeEach
  void setUp() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    var loginClient = new ClientRepresentation();
    loginClient.setId(CLIENT_UUID);
    when(keycloakClientService.getLoginClient()).thenReturn(loginClient);
  }

  @AfterEach
  void afterEach() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0],
      StandardCharsets.UTF_8);
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    void positive() {
      var expectedPolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();
      when(keycloakAdminClient.getPolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString())).thenReturn(keycloakPolicy);
      when(keycloakPolicyMapper.toPolicy(keycloakPolicy)).thenReturn(expectedPolicy);

      var policy = keycloakPolicyService.getById(POLICY_ID);

      assertThat(policy).isEqualTo(expectedPolicy);
    }

    @Test
    void negative_throwsNotFoundException() {
      when(keycloakAdminClient.getPolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString()))
        .thenThrow(httpError(NOT_FOUND));

      assertThatThrownBy(() -> keycloakPolicyService.getById(POLICY_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Failed to find policy: id = %s", POLICY_ID);
    }

    @Test
    void negative_throwsClientErrorException() {
      when(keycloakAdminClient.getPolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString()))
        .thenThrow(httpError(INTERNAL_SERVER_ERROR));

      assertThatThrownBy(() -> keycloakPolicyService.getById(POLICY_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to find policy: id = %s", POLICY_ID);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var timePolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();

      when(keycloakPolicyMapper.toKeycloakPolicy(timePolicy, PolicyMapperContext.empty())).thenReturn(keycloakPolicy);
      doNothing().when(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);

      keycloakPolicyService.create(timePolicy);

      verify(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);
    }

    @Test
    void positive_policyExistsByName() {
      var timePolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();

      when(keycloakPolicyMapper.toKeycloakPolicy(timePolicy, PolicyMapperContext.empty())).thenReturn(keycloakPolicy);
      doThrow(httpError(CONFLICT)).when(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);

      keycloakPolicyService.create(timePolicy);

      verify(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);
    }

    @Test
    void positive_userPolicy() {
      var userPolicy = userPolicy();
      var policyMapperContext = new PolicyMapperContext().keycloakUserIds(List.of(KEYCLOAK_USER_ID));
      var keycloakPolicy = keycloakUserPolicy();

      when(userService.findKeycloakIdByUserId(USER_ID)).thenReturn(KEYCLOAK_USER_ID);
      when(keycloakPolicyMapper.toKeycloakPolicy(userPolicy, policyMapperContext)).thenReturn(keycloakPolicy);
      doThrow(httpError(CONFLICT)).when(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);

      keycloakPolicyService.create(userPolicy);

      verify(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);
    }

    @Test
    void positive_notAuthorizedException() {
      var timePolicy = timePolicy();
      var keycloakPolicy = keycloakTimePolicy();

      when(keycloakPolicyMapper.toKeycloakPolicy(timePolicy, PolicyMapperContext.empty())).thenReturn(keycloakPolicy);
      doThrow(httpError(UNAUTHORIZED)).when(keycloakAdminClient).createPolicy(TENANT_ID, CLIENT_UUID, keycloakPolicy);

      assertThatThrownBy(() -> keycloakPolicyService.create(timePolicy))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Error during policy creation in Keycloak. Details: id = %s, status = %s, message = %s",
          POLICY_ID, 401, "Unauthorized");
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

      when(keycloakAdminClient.findPolicies(TENANT_ID, CLIENT_UUID, query, false, 5, 10))
        .thenReturn(List.of(keycloakTimePolicy, keycloakUserPolicy));
      when(keycloakPolicyMapper.toPolicy(keycloakTimePolicy)).thenReturn(timePolicy);
      when(keycloakPolicyMapper.toPolicy(keycloakUserPolicy)).thenReturn(userPolicy);

      var result = keycloakPolicyService.find(query, 10, 5);

      assertThat(result).containsExactly(timePolicy, userPolicy);
    }

    @Test
    void positive_emptyResults() {
      var query = "test";

      when(keycloakAdminClient.findPolicies(TENANT_ID, CLIENT_UUID, query, false, 5, 10)).thenReturn(emptyList());

      var result = keycloakPolicyService.find(query, 10, 5);

      assertThat(result).isEmpty();
    }

    @Test
    void positive_webApplicationException() {
      var query = "test";

      when(keycloakAdminClient.findPolicies(TENANT_ID, CLIENT_UUID, query, false, 5, 10))
        .thenThrow(httpError(UNAUTHORIZED));

      assertThatThrownBy(() -> keycloakPolicyService.find(query, 10, 5))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to search policies");
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
      doNothing().when(keycloakAdminClient)
        .updatePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString(), keycloakTimePolicy);

      keycloakPolicyService.update(policy);

      verify(keycloakAdminClient).updatePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString(), keycloakTimePolicy);
    }

    @Test
    void negative_internalServerErrorException() {
      var policy = timePolicy();
      var keycloakTimePolicy = keycloakTimePolicy();

      when(keycloakPolicyMapper.toKeycloakPolicy(policy, PolicyMapperContext.empty())).thenReturn(keycloakTimePolicy);
      doThrow(httpError(INTERNAL_SERVER_ERROR)).when(keycloakAdminClient)
        .updatePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString(), keycloakTimePolicy);

      assertThatThrownBy(() -> keycloakPolicyService.update(policy))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to update policy")
        .satisfies(err -> assertThat(((KeycloakApiException) err).getStatus()).isEqualTo(NOT_FOUND));
    }

    @Test
    void negative_unauthorizedException() {
      var policy = timePolicy();
      var keycloakTimePolicy = keycloakTimePolicy();

      when(keycloakPolicyMapper.toKeycloakPolicy(policy, PolicyMapperContext.empty())).thenReturn(keycloakTimePolicy);
      doThrow(httpError(UNAUTHORIZED)).when(keycloakAdminClient)
        .updatePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString(), keycloakTimePolicy);

      assertThatThrownBy(() -> keycloakPolicyService.update(policy))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to update policy")
        .satisfies(err -> assertThat(((KeycloakApiException) err).getStatus()).isEqualTo(UNAUTHORIZED));
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      doNothing().when(keycloakAdminClient).deletePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString());

      keycloakPolicyService.deleteById(POLICY_ID);

      verify(keycloakAdminClient).deletePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString());
    }

    @Test
    void negative_notFoundException() {
      doThrow(httpError(NOT_FOUND)).when(keycloakAdminClient)
        .deletePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString());

      assertThatThrownBy(() -> keycloakPolicyService.deleteById(POLICY_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Failed to find policy: id = %s", POLICY_ID);
    }

    @Test
    void negative_notAuthorizedException() {
      doThrow(httpError(UNAUTHORIZED)).when(keycloakAdminClient)
        .deletePolicyById(TENANT_ID, CLIENT_UUID, POLICY_ID.toString());

      assertThatThrownBy(() -> keycloakPolicyService.deleteById(POLICY_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to delete policy")
        .satisfies(err -> assertThat(((KeycloakApiException) err).getStatus()).isEqualTo(UNAUTHORIZED));
    }
  }
}
