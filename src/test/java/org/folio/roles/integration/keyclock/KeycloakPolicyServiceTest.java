package org.folio.roles.integration.keyclock;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.support.KeycloakUtils.ACCESS_TOKEN;
import static org.folio.roles.support.PolicyUtils.DAY_OF_MONTH_END;
import static org.folio.roles.support.PolicyUtils.DAY_OF_MONTH_START;
import static org.folio.roles.support.PolicyUtils.EXPIRE;
import static org.folio.roles.support.PolicyUtils.HOUR_END;
import static org.folio.roles.support.PolicyUtils.HOUR_START;
import static org.folio.roles.support.PolicyUtils.MINUTE_START;
import static org.folio.roles.support.PolicyUtils.MONTH_END;
import static org.folio.roles.support.PolicyUtils.MONTH_START;
import static org.folio.roles.support.PolicyUtils.POLICY_DESCRIPTION;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_NAME;
import static org.folio.roles.support.PolicyUtils.START;
import static org.folio.roles.support.PolicyUtils.createKeycloakTimePolicy;
import static org.folio.roles.support.PolicyUtils.createKeycloakTimePolicyWithConfig;
import static org.folio.roles.support.PolicyUtils.createResponseKeycloakUserPolicy;
import static org.folio.roles.support.PolicyUtils.createTimePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.TestConstants.LOGIN_CLIENT_SUFFIX;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.codec.DecodeException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.client.PolicyClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakPolicyMapper;
import org.folio.roles.mapper.KeycloakPolicyMapperImpl;
import org.folio.roles.support.TestConstants;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakPolicyServiceTest {

  private static final UUID KEYCLOAK_USER_ID = fromString("00000000-0000-0000-0000-000000000002");
  private static final String LOGIN_CLIENT_ID = TENANT_ID + LOGIN_CLIENT_SUFFIX;

  @Mock private PolicyClient client;
  @Mock private KeycloakAccessTokenService tokenService;
  @Mock private KeycloakClientService clientService;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakUserService userService;
  @Spy private KeycloakPolicyMapper mapper = new KeycloakPolicyMapperImpl();

  @InjectMocks private KeycloakPolicyService keycloakPolicyService;

  @BeforeEach
  void setUp() {
    when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
    when(context.getTenantId()).thenReturn(TestConstants.TENANT_ID);
    when(clientService.findAndCacheLoginClientUuid()).thenReturn(LOGIN_CLIENT_ID);
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(client, tokenService, context);
  }

  @Nested
  @DisplayName("findById")
  class FindByIdTest {

    @Test
    void positive() {
      var expectedPolicy = createKeycloakTimePolicyWithConfig();
      when(client.getById(anyString(), anyString(), anyString(), any(UUID.class))).thenReturn(expectedPolicy);

      var policy = keycloakPolicyService.findById(POLICY_ID);

      assertAll(() -> {
        assertEquals(POLICY_NAME, policy.getName());
        assertEquals(POLICY_ID, policy.getId());
        assertEquals(POLICY_DESCRIPTION, policy.getDescription());
        assertEquals(TIME, policy.getType());
        assertTrue(nonNull(policy.getTimePolicy()));

        var timePolicy = policy.getTimePolicy();
        assertEquals(START, timePolicy.getStart());
        assertEquals(EXPIRE, timePolicy.getExpires());
        assertEquals(DAY_OF_MONTH_START, timePolicy.getDayOfMonthStart());
        assertEquals(DAY_OF_MONTH_END, timePolicy.getDayOfMonthEnd());
        assertEquals(MONTH_START, timePolicy.getMonthStart());
        assertEquals(MONTH_END, timePolicy.getMonthEnd());
        assertEquals(HOUR_START, timePolicy.getHourStart());
        assertEquals(HOUR_END, timePolicy.getHourEnd());
        assertEquals(MINUTE_START, timePolicy.getMinuteStart());
        assertEquals(MONTH_END, timePolicy.getMinuteEnd());
      });
      verify(client).getById(anyString(), anyString(), anyString(), any(UUID.class));
    }

    @Test
    void negative_throwsEntityNotFoundExceptionIfNotFeignNotFound() {
      when(client.getById(anyString(), anyString(), anyString(), any(UUID.class))).thenThrow(
        FeignException.NotFound.class);

      assertThrows(EntityNotFoundException.class, () -> keycloakPolicyService.findById(POLICY_ID));
    }
  }

  @Nested
  @DisplayName("createSafe")
  class Create {

    @Test
    void positive() {
      var timePolicy = createTimePolicy();
      keycloakPolicyService.create(timePolicy);

      verify(clientService).findAndCacheLoginClientUuid();
      verify(tokenService).getToken();

      var keycloakTimePolicy = createKeycloakTimePolicy();
      verify(client).create(ACCESS_TOKEN, TENANT_ID, LOGIN_CLIENT_ID, "time", keycloakTimePolicy);
    }

    @Test
    void create_positive_alreadyExists() {
      var policy = createTimePolicy();

      doThrow(FeignException.Conflict.class).when(client)
        .create(anyString(), any(), any(), any(), any());
      assertDoesNotThrow(() -> keycloakPolicyService.create(policy));
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_returns_all_if_null_parameters() {
      var userPolicy = userPolicy();
      var timePolicy = createTimePolicy();
      userPolicy.setSource(null);
      timePolicy.setSource(null);
      var responseKeycloakUserPolicy = createResponseKeycloakUserPolicy();
      var keycloakTimePolicyWithConfig = createKeycloakTimePolicyWithConfig();

      when(client.findAll(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString())).thenReturn(
        List.of(responseKeycloakUserPolicy, keycloakTimePolicyWithConfig));

      var policies = keycloakPolicyService.search("role", 0, 10);

      assertEquals(List.of(userPolicy, timePolicy), policies);
      assertEquals(2, policies.size());
    }

    @Test
    void positive_returns_empty_result() {
      when(client.findAll(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString())).thenReturn(
        List.of());

      assertEquals(keycloakPolicyService.search("test-search", 0, 10), List.of());
    }

    @Test
    void negative_throws_api_exception() {
      doThrow(FeignException.class).when(client)
        .findAll(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

      assertThrows(KeycloakApiException.class, () -> keycloakPolicyService.search("test-search", 0, 10));
    }

    @Test
    void negative_throwsDecodeException() {
      doThrow(DecodeException.class).when(client)
        .findAll(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

      assertThrows(KeycloakApiException.class, () -> keycloakPolicyService.search("test-search", 0, 10));
    }
  }

  @Nested
  @DisplayName("updateById")
  class UpdateById {

    @Test
    void positive() {
      var policy = createTimePolicy();
      var keycloakTimePolicy = createKeycloakTimePolicy();
      keycloakPolicyService.update(policy);

      verify(client).updateById(anyString(), anyString(), anyString(), eq(policy.getType()
        .name()
        .toLowerCase()), eq(policy.getId()), eq(keycloakTimePolicy));
    }

    @Test
    void negative_throws_api_exception() {
      var policy = userPolicy();

      when(userService.findKeycloakIdByUserId(policy.getUserPolicy()
        .getUsers()
        .get(0))).thenReturn(KEYCLOAK_USER_ID);
      doThrow(FeignException.class).when(client)
        .updateById(anyString(), anyString(), anyString(), anyString(), any(UUID.class), any());

      assertThrows(KeycloakApiException.class, () -> keycloakPolicyService.update(policy));
      verify(client).updateById(anyString(), anyString(), anyString(), anyString(), any(UUID.class), any());
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      keycloakPolicyService.deleteById(POLICY_ID);

      verify(client).deleteById(anyString(), anyString(), anyString(), eq(POLICY_ID));
    }

    @Test
    void negative_throws_api_exception() {

      doThrow(FeignException.class).when(client)
        .deleteById(anyString(), anyString(), anyString(), eq(POLICY_ID));

      assertThrows(KeycloakApiException.class, () -> keycloakPolicyService.deleteById(POLICY_ID));
      verify(client).deleteById(anyString(), anyString(), anyString(), eq(POLICY_ID));
    }
  }
}
