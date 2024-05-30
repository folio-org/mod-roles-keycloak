package org.folio.roles.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.model.LogicType.POSITIVE;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_DESCRIPTION;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_NAME;
import static org.folio.roles.support.PolicyUtils.createTimePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.dto.Metadata;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyEntity;
import org.folio.roles.domain.entity.TimePolicyEntity;
import org.folio.roles.domain.entity.UserPolicyEntity;
import org.folio.roles.mapper.entity.DateConvertHelper;
import org.folio.roles.mapper.entity.PolicyEntityMapper;
import org.folio.roles.mapper.entity.PolicyEntityMapperImpl;
import org.folio.roles.repository.PolicyEntityRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PolicyEntityServiceTest {

  @InjectMocks private PolicyEntityService service;
  @Mock private PolicyEntityRepository repository;
  @Spy private PolicyEntityMapper mapper = new PolicyEntityMapperImpl(new DateConvertHelper());

  private static UserPolicyEntity userPolicyEntity() {
    var userPolicyEntity = new UserPolicyEntity();
    userPolicyEntity.setId(POLICY_ID);
    userPolicyEntity.setName(POLICY_NAME);
    userPolicyEntity.setDescription(POLICY_DESCRIPTION);
    userPolicyEntity.setUsers(List.of(USER_ID));
    userPolicyEntity.setLogic(POSITIVE);
    return userPolicyEntity;
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var timePolicy = createTimePolicy();
      var basePolicyEntity = mapper.toPolicyEntity(timePolicy);

      when(repository.findByName(POLICY_NAME)).thenReturn(Optional.empty());
      when(repository.save(basePolicyEntity)).thenReturn(basePolicyEntity);

      var policy = service.create(timePolicy);

      assertEquals(timePolicy.getId(), policy.getId());
      assertEquals(timePolicy.getTimePolicy(), policy.getTimePolicy());
      assertEquals(PolicyType.TIME, policy.getType());
    }

    @Test
    void negative_repositoryThrowsException() {
      when(repository.findByName(POLICY_NAME)).thenReturn(Optional.empty());
      when(repository.save(any(BasePolicyEntity.class))).thenThrow(RuntimeException.class);

      var policy = createTimePolicy();
      assertThrows(RuntimeException.class, () -> service.create(policy));
    }

    @Test
    void negative_nameIsTaken() {
      var userPolicy = userPolicy();
      when(repository.findByName(POLICY_NAME)).thenReturn(Optional.of(userPolicyEntity()));
      assertThatThrownBy(() -> service.create(userPolicy))
        .isInstanceOf(EntityExistsException.class)
        .hasMessage("Policy name is taken: %s", POLICY_NAME);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var timePolicy = createTimePolicy();
      var updatedName = "updated-name";
      timePolicy.setName(updatedName);
      var basePolicyEntity = mapper.toPolicyEntity(timePolicy);

      when(repository.save(any(BasePolicyEntity.class))).thenReturn(basePolicyEntity);
      var updated = service.update(timePolicy);

      assertEquals(timePolicy.getId(), updated.getId());
      assertEquals(updatedName, updated.getName());
      assertEquals(timePolicy.getTimePolicy(), updated.getTimePolicy());
      assertEquals(PolicyType.TIME, updated.getType());
    }

    @Test
    void negative_entityNotFound() {
      var userPolicy = userPolicy();

      when(repository.getReferenceById(any(UUID.class))).thenThrow(EntityNotFoundException.class);

      assertThrows(EntityNotFoundException.class, () -> service.update(userPolicy));
    }

    @Test
    void negative_policyIdIsNull() {
      var timePolicy = createTimePolicy();
      timePolicy.setId(null);

      assertThrowsExactly(NullPointerException.class, () -> service.update(timePolicy),
        "To update policy, policy ID cannot be null");
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      service.deleteById(POLICY_ID);

      verify(repository).deleteById(POLICY_ID);
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    @Test
    void positive() {
      var timePolicy = createTimePolicy();
      var basePolicyEntity = mapper.toPolicyEntity(timePolicy);

      when(repository.findById(POLICY_ID)).thenReturn(Optional.of(basePolicyEntity));

      var policy = service.findById(POLICY_ID);

      assertEquals(policy.getId(), basePolicyEntity.getId());
      assertEquals(policy.getName(), basePolicyEntity.getName());
      assertEquals(policy.getDescription(), basePolicyEntity.getDescription());
      assertEquals(TimePolicyEntity.class, basePolicyEntity.getClass());
    }

    @Test
    void negative_entityNotFound() {
      when(repository.findById(any(UUID.class))).thenThrow(EntityNotFoundException.class);

      assertThrows(EntityNotFoundException.class, () -> service.findById(POLICY_ID),
        "Policy not found: id = " + POLICY_ID);
    }
  }

  @Nested
  @DisplayName("findByQuery")
  class FindByQuery {

    @Test
    void positive_queryNotExists() {
      when(repository.findAll(any(OffsetRequest.class))).thenReturn(Page.empty());

      service.findByQuery(null, 10, 10);

      verify(repository, never()).findByCql(anyString(), any(OffsetRequest.class));
    }

    @Test
    void positive_queryExists() {
      when(repository.findByCql(anyString(), any(OffsetRequest.class))).thenReturn(Page.empty());

      service.findByQuery("query", 10, 10);

      verify(repository, never()).findAll(any(OffsetRequest.class));
    }

    @Test
    void positive() {
      var offset = 0;
      var limit = 10;
      var cqlQuery = "cql.allRecords = 1";
      var timePolicy = createTimePolicy();
      var basePolicyEntity = mapper.toPolicyEntity(timePolicy);
      var policies = List.of(basePolicyEntity);
      var expectedPage = new PageImpl<>(policies, Pageable.ofSize(1), 1);

      when(repository.findByCql(cqlQuery, OffsetRequest.of(offset, limit))).thenReturn(expectedPage);

      var result = service.findByQuery(cqlQuery, offset, limit);

      assertEquals(1, result.size());
      assertEquals(timePolicy.getId(), result.get(0).getId());
      assertEquals(timePolicy.getName(), result.get(0).getName());
      assertEquals(timePolicy.getDescription(), result.get(0).getDescription());
      assertEquals(PolicyType.TIME, result.get(0).getType());
    }
  }

  @Nested
  @DisplayName("existsById")
  class ExistsById {

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void positive(boolean entityExistsResult) {
      when(repository.existsById(POLICY_ID)).thenReturn(entityExistsResult);
      var result = service.existsById(POLICY_ID);
      assertThat(result).isEqualTo(entityExistsResult);
    }
  }

  @Nested
  @DisplayName("findByName")
  class FindByName {

    @Test
    void positive() {
      when(repository.findByName(POLICY_NAME)).thenReturn(Optional.of(userPolicyEntity()));
      var result = service.findByName(POLICY_NAME);
      assertThat(result).isPresent().contains(userPolicy().metadata(new Metadata()));
    }
  }

  @Nested
  @DisplayName("findRolePoliciesByCapabilityId")
  class FindRolePoliciesByCapabilityId {

    @Test
    void positive() {
      var entity = new RolePolicyEntity();
      var expectedPolicy = new Policy();
      when(repository.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(entity));
      when(mapper.toRolePolicy(entity)).thenReturn(expectedPolicy);

      var result = service.findRolePoliciesByCapabilityId(CAPABILITY_ID);

      assertThat(result).containsExactly(expectedPolicy);
    }
  }

  @Nested
  @DisplayName("findRolePoliciesByCapabilitySetId")
  class FindRolePoliciesByCapabilitySetId {

    @Test
    void positive() {
      var entity = new RolePolicyEntity();
      var expectedPolicy = new Policy();
      when(repository.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(entity));
      when(mapper.toRolePolicy(entity)).thenReturn(expectedPolicy);

      var result = service.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID);

      assertThat(result).containsExactly(expectedPolicy);
    }
  }

  @Nested
  @DisplayName("findUserPoliciesByCapabilityId")
  class FindUserPoliciesByCapabilityId {

    @Test
    void positive() {
      var entity = new UserPolicyEntity();
      var expectedPolicy = new Policy();
      when(repository.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(entity));
      when(mapper.toUserPolicy(entity)).thenReturn(expectedPolicy);

      var result = service.findUserPoliciesByCapabilityId(CAPABILITY_ID);

      assertThat(result).containsExactly(expectedPolicy);
    }
  }

  @Nested
  @DisplayName("findUserPoliciesByCapabilitySetId")
  class FindUserPoliciesByCapabilitySetId {

    @Test
    void positive() {
      var entity = new UserPolicyEntity();
      var expectedPolicy = new Policy();
      when(repository.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(entity));
      when(mapper.toUserPolicy(entity)).thenReturn(expectedPolicy);

      var result = service.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID);

      assertThat(result).containsExactly(expectedPolicy);
    }
  }
}
