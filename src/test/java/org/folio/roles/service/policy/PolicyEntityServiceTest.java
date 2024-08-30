package org.folio.roles.service.policy;

import static java.time.Instant.ofEpochSecond;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.model.LogicType.POSITIVE;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_DESCRIPTION;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.POLICY_NAME;
import static org.folio.roles.support.PolicyUtils.TIME_POLICY_NAME;
import static org.folio.roles.support.PolicyUtils.USER_POLICY_NAME;
import static org.folio.roles.support.PolicyUtils.createTimePolicy;
import static org.folio.roles.support.PolicyUtils.timePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyEntity;
import org.folio.roles.domain.entity.TimePolicyEntity;
import org.folio.roles.domain.entity.UserPolicyEntity;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.mapper.entity.PolicyEntityMapper;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PolicyEntityServiceTest {

  @InjectMocks private PolicyEntityService service;
  @Mock private PolicyEntityRepository repository;
  @Mock private PolicyEntityMapper mapper;

  private static UserPolicyEntity userPolicyEntity() {
    var userPolicyEntity = new UserPolicyEntity();
    userPolicyEntity.setId(POLICY_ID);
    userPolicyEntity.setName(POLICY_NAME);
    userPolicyEntity.setDescription(POLICY_DESCRIPTION);
    userPolicyEntity.setUsers(List.of(USER_ID));
    userPolicyEntity.setLogic(POSITIVE);
    userPolicyEntity.setSource(SourceType.USER);
    return userPolicyEntity;
  }

  private static TimePolicyEntity timePolicyEntity() {
    return timePolicyEntity(TIME_POLICY_NAME);
  }

  private static TimePolicyEntity timePolicyEntity(String name) {
    var timePolicyEntity = new TimePolicyEntity();
    timePolicyEntity.setId(POLICY_ID);
    timePolicyEntity.setName(name);
    timePolicyEntity.setStart(OffsetDateTime.ofInstant(ofEpochSecond(1724716800), ZoneId.of("UTC")));
    timePolicyEntity.setExpires(OffsetDateTime.ofInstant(ofEpochSecond(1724976000), ZoneId.of("UTC")));
    timePolicyEntity.setMonthStart(1);
    timePolicyEntity.setMonthEnd(3);
    timePolicyEntity.setRepeat(true);
    timePolicyEntity.setSource(SourceType.USER);
    return timePolicyEntity;
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var timePolicy = timePolicy();
      var timePolicyEntity = timePolicyEntity();

      when(mapper.toPolicyEntity(timePolicy)).thenReturn(timePolicyEntity);
      when(mapper.toPolicy(timePolicyEntity)).thenReturn(timePolicy);
      when(repository.findByName(TIME_POLICY_NAME)).thenReturn(Optional.empty());
      when(repository.save(timePolicyEntity)).thenReturn(timePolicyEntity);

      var result = service.create(timePolicy);
      assertThat(result).isEqualTo(timePolicy);
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
      when(repository.findByName(USER_POLICY_NAME)).thenReturn(Optional.of(userPolicyEntity()));
      assertThatThrownBy(() -> service.create(userPolicy))
        .isInstanceOf(EntityExistsException.class)
        .hasMessage("Policy name is taken: %s", USER_POLICY_NAME);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var updatedName = "result-name";
      var timePolicy = timePolicy().name(updatedName);
      var timePolicyEntity = timePolicyEntity();
      var updatedTimePolicyEntity = timePolicyEntity(updatedName);

      when(mapper.toPolicyEntity(timePolicy)).thenReturn(updatedTimePolicyEntity);
      when(mapper.toPolicy(updatedTimePolicyEntity)).thenReturn(timePolicy);
      when(repository.getReferenceById(POLICY_ID)).thenReturn(timePolicyEntity);
      when(repository.save(updatedTimePolicyEntity)).thenReturn(updatedTimePolicyEntity);

      var result = service.update(timePolicy);

      assertThat(result).isEqualTo(timePolicy);
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
  @DisplayName("getById")
  class GetById {

    @Test
    void positive() {
      var timePolicy = timePolicy();
      var timePolicyEntity = timePolicyEntity();

      when(mapper.toPolicy(timePolicyEntity)).thenReturn(timePolicy);
      when(repository.findById(POLICY_ID)).thenReturn(Optional.of(timePolicyEntity));

      var result = service.getById(POLICY_ID);

      assertThat(result).isEqualTo(timePolicy);
    }

    @Test
    void negative_entityNotFound() {
      when(repository.findById(POLICY_ID)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getById(POLICY_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Policy not found: id = %s", POLICY_ID);
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

      var timePolicyEntity = timePolicyEntity();
      var userPolicyEntity = userPolicyEntity();
      var foundEntities = List.of(timePolicyEntity, userPolicyEntity);

      when(mapper.toPolicy(timePolicyEntity)).thenReturn(timePolicy());
      when(mapper.toPolicy(userPolicyEntity)).thenReturn(userPolicy());
      when(repository.findByCql(cqlQuery, OffsetRequest.of(offset, limit))).thenReturn(new PageImpl<>(foundEntities));

      var result = service.findByQuery(cqlQuery, offset, limit);

      assertThat(result).isEqualTo(PageResult.asSinglePage(timePolicy(), userPolicy()));
    }

    @Test
    void positive_blankQuery() {
      var offset = 0;
      var limit = 10;
      var cqlQuery = " ";

      var timePolicyEntity = timePolicyEntity();
      var userPolicyEntity = userPolicyEntity();
      var foundEntities = List.of(timePolicyEntity, userPolicyEntity);

      when(mapper.toPolicy(timePolicyEntity)).thenReturn(timePolicy());
      when(mapper.toPolicy(userPolicyEntity)).thenReturn(userPolicy());
      when(repository.findAll(OffsetRequest.of(offset, limit))).thenReturn(new PageImpl<>(foundEntities));

      var result = service.findByQuery(cqlQuery, offset, limit);

      assertThat(result).isEqualTo(PageResult.asSinglePage(timePolicy(), userPolicy()));
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
      when(repository.findByName(USER_POLICY_NAME)).thenReturn(Optional.of(userPolicyEntity()));
      when(mapper.toPolicy(userPolicyEntity())).thenReturn(userPolicy());

      var result = service.findByName(USER_POLICY_NAME);

      assertThat(result).isPresent().contains(userPolicy());
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
