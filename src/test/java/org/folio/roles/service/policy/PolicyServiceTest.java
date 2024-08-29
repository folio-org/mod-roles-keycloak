package org.folio.roles.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.domain.dto.PolicyType.USER;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_NAME;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.roles.support.PolicyUtils.timePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.keyclock.KeycloakPolicyService;
import org.folio.roles.support.PolicyUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

  @InjectMocks private PolicyService service;

  @Mock private KeycloakPolicyService keycloakService;
  @Mock private PolicyEntityService entityService;
  @Mock private TransactionTemplate transactionTemplate;

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    void positive() {
      var timePolicy = timePolicy();
      when(entityService.getById(POLICY_ID)).thenReturn(timePolicy);

      var policy = service.findById(POLICY_ID);

      assertEquals(TIME, policy.getType());
      assertEquals(timePolicy.getId(), policy.getId());
      verifyNoMoreInteractions(entityService);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var timePolicy = timePolicy().id(null);
      var policies = List.of(timePolicy);

      when(entityService.create(timePolicy)).thenReturn(timePolicy);
      doNothing().when(keycloakService).create(timePolicy);

      when(transactionTemplate.execute(any())).thenAnswer(inv ->
        inv.<TransactionCallback<Policy>>getArgument(0).doInTransaction(new SimpleTransactionStatus()));

      var result = service.create(policies);

      assertThat(result).isEqualTo(PageResult.asSinglePage(timePolicy));
    }

    @Test
    void positive_singlePolicy() {
      var timePolicy = timePolicy().id(null);

      when(entityService.create(timePolicy)).thenReturn(timePolicy);
      doNothing().when(keycloakService).create(timePolicy);

      var result = service.create(timePolicy);

      assertThat(result).isEqualTo(timePolicy);
    }
  }

  @Nested
  @DisplayName("getOrCreatePolicy")
  class GetOrCreatePolicy {

    private static final Supplier<Policy> USER_POLICY_SUPPLIER = PolicyUtils::userPolicy;

    @Test
    void positive_notFoundByName() {
      var userPolicy = userPolicy();
      when(entityService.findByName(POLICY_NAME)).thenReturn(Optional.empty());
      when(entityService.create(userPolicy)).thenReturn(userPolicy);
      doNothing().when(keycloakService).create(userPolicy);

      var result = service.getOrCreatePolicy(POLICY_NAME, USER, USER_POLICY_SUPPLIER);

      assertThat(result).isEqualTo(userPolicy);
    }

    @Test
    void positive_foundByName() {
      var userPolicy = userPolicy();
      when(entityService.findByName(POLICY_NAME)).thenReturn(Optional.of(userPolicy));

      var result = service.getOrCreatePolicy(POLICY_NAME, USER, USER_POLICY_SUPPLIER);

      assertThat(result).isEqualTo(userPolicy);
    }

    @Test
    void negative_foundByNameWithInvalidType() {
      when(entityService.findByName(POLICY_NAME)).thenReturn(Optional.of(rolePolicy()));

      assertThatThrownBy(() -> service.getOrCreatePolicy(POLICY_NAME, USER, USER_POLICY_SUPPLIER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Type is incorrect for policy: %s, expected: %s but actual is %s", POLICY_ID, USER, ROLE);
    }
  }

  @Nested
  @DisplayName("existsById")
  class ExistsById {

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void positive(boolean entityExistsResult) {
      when(entityService.existsById(POLICY_ID)).thenReturn(entityExistsResult);
      var result = service.existsById(POLICY_ID);
      assertThat(result).isEqualTo(entityExistsResult);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var userPolicy = userPolicy();
      service.update(userPolicy);

      verify(entityService).update(userPolicy);
      verify(keycloakService).update(userPolicy);
    }
  }

  @Nested
  @DisplayName("getByNameAndType")
  class GetByNameAndType {

    @Test
    void positive() {
      var userPolicy = userPolicy();
      when(entityService.findByName(POLICY_NAME)).thenReturn(Optional.of(userPolicy));
      var result = service.getByNameAndType(POLICY_NAME, USER);
      assertThat(result).isEqualTo(userPolicy);
    }

    @Test
    void negative_notFound() {
      when(entityService.findByName(POLICY_NAME)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getByNameAndType(POLICY_NAME, ROLE))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Role policy is not found by name: '%s' and type: %s", POLICY_NAME, ROLE);
    }

    @Test
    void negative_typeIsInvalid() {
      var userPolicy = userPolicy();
      when(entityService.findByName(POLICY_NAME)).thenReturn(Optional.of(userPolicy));
      assertThatThrownBy(() -> service.getByNameAndType(POLICY_NAME, ROLE))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Role policy is not found by name: '%s' and type: %s", POLICY_NAME, ROLE);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive() {
      var timePolicy = timePolicy();
      var userPolicy = userPolicy();
      var policiesPage = PageResult.asSinglePage(timePolicy, userPolicy);
      var cqlQuery = "cql.allRecords = 1";

      when(entityService.findByQuery(cqlQuery, 0, 2)).thenReturn(policiesPage);

      var result = service.search(cqlQuery, 2, 0);

      assertThat(result).isEqualTo(policiesPage);
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      service.deleteById(POLICY_ID);
      verify(entityService).deleteById(POLICY_ID);
      verify(keycloakService).deleteById(POLICY_ID);
    }

    @Test
    void negative_keycloakException() {
      doThrow(EntityNotFoundException.class).when(keycloakService).deleteById(POLICY_ID);
      service.deleteById(POLICY_ID);
      verify(entityService).deleteById(POLICY_ID);
    }
  }

  @Nested
  @DisplayName("findRolePoliciesByCapabilityId")
  class FindRolePoliciesByCapabilityId {

    @Test
    void positive() {
      var expectedPolicy = new Policy();
      when(entityService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(expectedPolicy));
      var result = service.findRolePoliciesByCapabilityId(CAPABILITY_ID);
      assertThat(result).containsExactly(expectedPolicy);
    }
  }

  @Nested
  @DisplayName("findRolePoliciesByCapabilitySetId")
  class FindRolePoliciesByCapabilitySetId {

    @Test
    void positive() {
      var expectedPolicy = new Policy();
      when(entityService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(expectedPolicy));
      var result = service.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID);
      assertThat(result).containsExactly(expectedPolicy);
    }
  }

  @Nested
  @DisplayName("findUserPoliciesByCapabilityId")
  class FindUserPoliciesByCapabilityId {

    @Test
    void positive() {
      var expectedPolicy = new Policy();
      when(entityService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(expectedPolicy));
      var result = service.findUserPoliciesByCapabilityId(CAPABILITY_ID);
      assertThat(result).containsExactly(expectedPolicy);
    }
  }

  @Nested
  @DisplayName("findUserPoliciesByCapabilitySetId")
  class FindUserPoliciesByCapabilitySetId {

    @Test
    void positive() {
      var expectedPolicy = new Policy();
      when(entityService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(expectedPolicy));
      var result = service.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID);
      assertThat(result).containsExactly(expectedPolicy);
    }
  }
}
