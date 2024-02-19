package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.entity.CapabilitySetEntity.DEFAULT_CAPABILITY_SET_SORT;
import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.service.event.DomainEventType.CREATE;
import static org.folio.roles.service.event.DomainEventType.DELETE;
import static org.folio.roles.service.event.DomainEventType.UPDATE;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.RESOURCE_NAME;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.roles.service.event.DomainEvent;
import org.folio.roles.service.event.DomainEventType;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilitySetServiceTest {

  @InjectMocks private CapabilitySetService capabilitySetService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetRepository capabilitySetRepository;
  @Mock private CapabilitySetEntityMapper mapper;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private ApplicationEventPublisher eventPublisher;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static Consumer<DomainEvent<CapabilitySet>> isDomainEventOf(DomainEventType type, CapabilitySet newValue,
    CapabilitySet oldValue) {
    return event -> {
      assertThat(event.getType()).isEqualTo(type);
      assertThat(event.getNewObject()).isEqualTo(newValue);
      assertThat(event.getOldObject()).isEqualTo(oldValue);
    };
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var capabilitySet = capabilitySet();
      var capabilitySetEntity = capabilitySetEntity();

      doNothing().when(capabilityService).checkIds(List.of(CAPABILITY_ID));
      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(false);
      when(mapper.convert(capabilitySet)).thenReturn(capabilitySetEntity);
      when(capabilitySetRepository.save(capabilitySetEntity)).thenReturn(capabilitySetEntity);
      when(mapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);

      doNothing().when(eventPublisher).publishEvent(assertArg(isDomainEventOf(CREATE, capabilitySet, null)));

      var actual = capabilitySetService.create(capabilitySet);

      assertThat(actual).isEqualTo(capabilitySet);
    }

    @Test
    void negative_capabilityIdNotFound() {
      doThrow(EntityNotFoundException.class).when(capabilityService).checkIds(List.of(CAPABILITY_ID));
      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(false);

      var capabilitySet = capabilitySet();
      assertThatThrownBy(() -> capabilitySetService.create(capabilitySet))
        .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void negative_nameIsTaken() {
      var capabilityToCreate = capabilitySet();

      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(true);

      assertThatThrownBy(() -> capabilitySetService.create(capabilityToCreate))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("Capability set name is already taken")
        .satisfies(err -> {
          var validationException = (RequestValidationException) err;
          assertThat(validationException.getKey()).isEqualTo("name");
          assertThat(validationException.getValue()).isEqualTo("test_resource.create");
        });
    }

    @Test
    void positive_batchRequest() {
      var capabilitySet = capabilitySet();
      var capabilitySetEntity = capabilitySetEntity();

      doNothing().when(capabilityService).checkIds(List.of(CAPABILITY_ID));
      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(false);
      when(mapper.convert(capabilitySet)).thenReturn(capabilitySetEntity);
      when(capabilitySetRepository.save(capabilitySetEntity)).thenReturn(capabilitySetEntity);
      when(mapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);

      doNothing().when(eventPublisher).publishEvent(assertArg(isDomainEventOf(CREATE, capabilitySet, null)));

      var capabilitySets = List.of(capabilitySet);
      var actual = capabilitySetService.create(capabilitySets);

      assertThat(actual).isEqualTo(capabilitySets);
    }

    @Test
    void positive_batchRequest_emptySets() {
      var actual = capabilitySetService.create(emptyList());
      assertThat(actual).isEmpty();
    }

    @Test
    void positive_batchRequest_nameIsTaken() {
      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(true);

      var capabilitySets = List.of(capabilitySet());
      var actual = capabilitySetService.create(capabilitySets);

      assertThat(actual).isEmpty();
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SET_SORT);

    @Test
    void positive() {
      var capabilitySet = capabilitySet();
      var capabilitySetEntity = capabilitySetEntity();
      var page = new PageImpl<>(List.of(capabilitySetEntity));
      when(capabilitySetRepository.findByQuery(null, offsetRequest)).thenReturn(page);
      when(mapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);

      var result = capabilitySetService.find(null, 10, 0);

      assertThat(result).isEqualTo(PageResult.asSinglePage(capabilitySet));
    }

    @Test
    void positive_cqlQuery() {
      var query = "cql.allRecords = 1";
      var capabilitySet = capabilitySet();
      var capabilitySetEntity = capabilitySetEntity();
      var page = new PageImpl<>(List.of(capabilitySetEntity));
      when(capabilitySetRepository.findByQuery(query, offsetRequest)).thenReturn(page);
      when(mapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);

      var result = capabilitySetService.find(query, 10, 0);

      assertThat(result).isEqualTo(PageResult.asSinglePage(capabilitySet));
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private final UUID updatedCapabilityId = UUID.randomUUID();
    private final List<UUID> updatedCapabilityIds = List.of(updatedCapabilityId);

    @Test
    void positive() {
      var foundEntity = capabilitySetEntity();
      var foundCapabilitySet = capabilitySet();
      var updatedEntity = capabilitySetEntity(updatedCapabilityIds);
      var updatedCapabilitySet = capabilitySet(updatedCapabilityIds);

      doNothing().when(capabilityService).checkIds(updatedCapabilityIds);
      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(mapper.convert(updatedCapabilitySet)).thenReturn(updatedEntity);
      when(mapper.convert(updatedEntity)).thenReturn(updatedCapabilitySet);
      when(mapper.convert(foundEntity)).thenReturn(foundCapabilitySet);
      when(capabilitySetRepository.saveAndFlush(updatedEntity)).thenReturn(updatedEntity);

      doNothing().when(eventPublisher).publishEvent(assertArg(isDomainEventOf(UPDATE, updatedCapabilitySet,
        foundCapabilitySet)));

      capabilitySetService.update(CAPABILITY_SET_ID, updatedCapabilitySet);
    }

    @Test
    void positive_setNameIfNull() {
      var foundEntity = capabilitySetEntity(updatedCapabilityIds);
      var foundCapabilitySet = capabilitySet(updatedCapabilityIds);
      var updatedEntity = capabilitySetEntity(updatedCapabilityIds);
      var updatedCapabilitySet = capabilitySet(updatedCapabilityIds).name(null);

      doNothing().when(capabilityService).checkIds(updatedCapabilityIds);
      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(mapper.convert(updatedCapabilitySet)).thenReturn(updatedEntity);
      when(mapper.convert(updatedEntity)).thenReturn(foundCapabilitySet);
      when(mapper.convert(foundEntity)).thenReturn(foundCapabilitySet);
      when(capabilitySetRepository.saveAndFlush(updatedEntity)).thenReturn(updatedEntity);

      doNothing().when(eventPublisher).publishEvent(assertArg(isDomainEventOf(UPDATE, updatedCapabilitySet,
        foundCapabilitySet)));

      capabilitySetService.update(CAPABILITY_SET_ID, updatedCapabilitySet);
    }

    @Test
    void positive_actionIsChanged() {
      var foundEntity = capabilitySetEntity(RESOURCE_NAME, org.folio.roles.domain.dto.CapabilityAction.CREATE);
      var foundCapabilitySet = capabilitySet(RESOURCE_NAME, org.folio.roles.domain.dto.CapabilityAction.CREATE);
      var updatedEntity = capabilitySetEntity(RESOURCE_NAME, EDIT);
      var updatedCapabilitySet = capabilitySet(RESOURCE_NAME, EDIT);

      doNothing().when(capabilityService).checkIds(List.of(CAPABILITY_ID));
      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(capabilitySetRepository.existsByName("test_resource.edit")).thenReturn(false);
      when(mapper.convert(updatedCapabilitySet)).thenReturn(updatedEntity);
      when(mapper.convert(updatedEntity)).thenReturn(updatedCapabilitySet);
      when(mapper.convert(foundEntity)).thenReturn(foundCapabilitySet);
      when(capabilitySetRepository.saveAndFlush(updatedEntity)).thenReturn(updatedEntity);

      doNothing().when(eventPublisher).publishEvent(assertArg(isDomainEventOf(UPDATE, updatedCapabilitySet,
        foundCapabilitySet)));

      capabilitySetService.update(CAPABILITY_SET_ID, updatedCapabilitySet);
    }

    @Test
    void positive_updatedCapabilityIdsAreNotFound() {
      var foundEntity = capabilitySetEntity();
      var capabilitySetToUpdate = capabilitySet(updatedCapabilityIds);

      doThrow(EntityNotFoundException.class).when(capabilityService).checkIds(updatedCapabilityIds);
      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);

      assertThatThrownBy(() -> capabilitySetService.update(CAPABILITY_SET_ID, capabilitySetToUpdate))
        .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void negative_actionIsChangedAndNewNameIsTaken() {
      var foundEntity = capabilitySetEntity(RESOURCE_NAME, org.folio.roles.domain.dto.CapabilityAction.CREATE);
      var capabilitySetToUpdate = capabilitySet(RESOURCE_NAME, EDIT);

      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(capabilitySetRepository.existsByName("test_resource.edit")).thenReturn(true);

      assertThatThrownBy(() -> capabilitySetService.update(CAPABILITY_SET_ID, capabilitySetToUpdate))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("Capability set name is already taken")
        .satisfies(err -> {
          var validationException = (RequestValidationException) err;
          assertThat(validationException.getKey()).isEqualTo("name");
          assertThat(validationException.getValue()).isEqualTo("test_resource.edit");
        });
    }

    @Test
    void negative_entityIdIsNull() {
      var capabilitySetToUpdate = capabilitySet(RESOURCE_NAME, EDIT).id(null);

      assertThatThrownBy(() -> capabilitySetService.update(CAPABILITY_SET_ID, capabilitySetToUpdate))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("must not be null")
        .satisfies(err -> {
          var validationException = (RequestValidationException) err;
          assertThat(validationException.getKey()).isEqualTo("id");
          assertThat(validationException.getValue()).isNull();
        });
    }

    @Test
    void negative_nonMatchingIds() {
      var capabilitySetId = UUID.randomUUID();
      var capabilitySetToUpdate = capabilitySet(RESOURCE_NAME, EDIT);

      assertThatThrownBy(() -> capabilitySetService.update(capabilitySetId, capabilitySetToUpdate))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("Id from path and in entity does not match", capabilitySetId)
        .satisfies(err -> {
          var validationException = (RequestValidationException) err;
          assertThat(validationException.getKey()).isEqualTo("id");
          assertThat(validationException.getValue()).isEqualTo(CAPABILITY_SET_ID.toString());
        });
    }
  }

  @Nested
  @DisplayName("getCapabilities")
  class GetCapabilities {

    @Test
    void positive() {
      var capabilityIds = List.of(CAPABILITY_SET_ID);
      var foundEntity = capabilitySetEntity();
      var expectedCapability = capabilitySet();

      when(capabilitySetRepository.findAllById(capabilityIds)).thenReturn(List.of(foundEntity));
      when(mapper.convert(List.of(foundEntity))).thenReturn(List.of(expectedCapability));

      var result = capabilitySetService.getCapabilities(capabilityIds);

      assertThat(result).containsExactly(expectedCapability);
    }

    @Test
    void positive_emptyListOfIds() {
      var result = capabilitySetService.getCapabilities(emptyList());
      assertThat(result).isEmpty();
    }

    @Test
    void negative_capabilityIsNotFoundById() {
      var capabilityIds = List.of(CAPABILITY_SET_ID);
      when(capabilitySetRepository.findAllById(capabilityIds)).thenReturn(emptyList());

      assertThatThrownBy(() -> capabilitySetService.getCapabilities(capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Capabilities are not found by ids: %s", capabilityIds);
    }
  }

  @Nested
  @DisplayName("findCapabilities")
  class FindCapabilities {

    @Test
    void positive() {
      var capabilityIds = List.of(CAPABILITY_SET_ID);
      var foundEntity = capabilitySetEntity();
      var expectedCapability = capabilitySet();

      when(capabilitySetRepository.findAllById(capabilityIds)).thenReturn(List.of(foundEntity));
      when(mapper.convert(List.of(foundEntity))).thenReturn(List.of(expectedCapability));

      var result = capabilitySetService.find(capabilityIds);

      assertThat(result).containsExactly(expectedCapability);
    }

    @Test
    void positive_emptyListOfIds() {
      var result = capabilitySetService.find(emptyList());
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("getCapability")
  class GetCapability {

    @Test
    void positive() {
      var foundEntity = capabilitySetEntity();
      var expectedCapability = capabilitySet();

      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(mapper.convert(foundEntity)).thenReturn(expectedCapability);

      var result = capabilitySetService.get(CAPABILITY_SET_ID);

      assertThat(result).isEqualTo(expectedCapability);
    }

    @Test
    void negative_capabilitySetIsNotFoundById() {
      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenThrow(EntityNotFoundException.class);

      assertThatThrownBy(() -> capabilitySetService.get(CAPABILITY_SET_ID))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findUserCapabilities")
  class FindUserCapabilities {

    @Test
    void positive_expandIsTrue() {
      var foundEntity = capabilitySetEntity();
      var expectedCapability = capabilitySet();
      when(capabilitySetRepository.findCapabilitiesForUser(USER_ID)).thenReturn(List.of(foundEntity));
      when(mapper.convert(List.of(foundEntity))).thenReturn(List.of(expectedCapability));

      var result = capabilitySetService.findUserCapabilities(USER_ID, false);

      assertThat(result).containsExactly(expectedCapability);
    }

    @Test
    void positive_expandIsFalse() {
      var foundEntity = capabilitySetEntity();
      var expectedCapabilitySet = capabilitySet();
      when(capabilitySetRepository.findExpandedCapabilitiesForUser(USER_ID)).thenReturn(List.of(foundEntity));
      when(mapper.convert(List.of(foundEntity))).thenReturn(List.of(expectedCapabilitySet));

      var result = capabilitySetService.findUserCapabilities(USER_ID, true);

      assertThat(result).containsExactly(expectedCapabilitySet);
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 100, DEFAULT_CAPABILITY_SET_SORT);

    @Test
    void positive() {
      var userCapabilitySetEntity = capabilitySetEntity();
      var pageResult = new PageImpl<>(List.of(userCapabilitySetEntity));
      var expectedCapabilitySet = capabilitySet();

      when(capabilitySetRepository.findByUserId(USER_ID, offsetRequest)).thenReturn(pageResult);
      when(mapper.convert(userCapabilitySetEntity)).thenReturn(expectedCapabilitySet);

      var result = capabilitySetService.findByUserId(USER_ID, 100, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedCapabilitySet));
    }
  }

  @Nested
  @DisplayName("findByRoleId")
  class FindByRoleId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 15, DEFAULT_CAPABILITY_SET_SORT);

    @Test
    void positive() {
      var userCapabilitySetEntity = capabilitySetEntity();
      var pageResult = new PageImpl<>(List.of(userCapabilitySetEntity));
      var expectedCapabilitySet = capabilitySet();

      when(capabilitySetRepository.findByRoleId(ROLE_ID, offsetRequest)).thenReturn(pageResult);
      when(mapper.convert(userCapabilitySetEntity)).thenReturn(expectedCapabilitySet);

      var result = capabilitySetService.findByRoleId(ROLE_ID, 15, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedCapabilitySet));
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    void positive() {
      var entity = capabilitySetEntity();
      var capabilitySet = capabilitySet();

      when(capabilitySetRepository.findById(CAPABILITY_SET_ID)).thenReturn(Optional.of(entity));
      when(mapper.convert(entity)).thenReturn(capabilitySet);
      doNothing().when(eventPublisher).publishEvent(assertArg(isDomainEventOf(DELETE, null, capabilitySet)));

      capabilitySetService.delete(CAPABILITY_SET_ID);

      verify(capabilitySetRepository).delete(entity);
    }

    @Test
    void negative_entityNotFound() {
      var entity = capabilitySetEntity();
      when(capabilitySetRepository.findById(CAPABILITY_SET_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> capabilitySetService.delete(CAPABILITY_SET_ID))
        .isInstanceOf(EntityNotFoundException.class);

      verify(capabilitySetRepository, never()).delete(entity);
    }
  }

  @Nested
  @DisplayName("existsByName")
  class ExistsByName {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void positive_parameterized(boolean exists) {
      var capabilityName = "test_resource.view";
      when(capabilitySetRepository.existsByName(capabilityName)).thenReturn(exists);
      var result = capabilitySetService.existsByName(capabilityName);
      assertThat(result).isEqualTo(exists);
    }
  }

  @Nested
  @DisplayName("checkCapabilityIds")
  class CheckCapabilityIds {

    @Test
    void positive() {
      var capabilityIds = List.of(CAPABILITY_SET_ID);
      var capabilityIdsSet = Set.of(CAPABILITY_SET_ID);
      when(capabilitySetRepository.findCapabilitySetIdsByIdIn(capabilityIdsSet)).thenReturn(capabilityIdsSet);

      capabilitySetService.checkIds(capabilityIds);

      verify(capabilitySetRepository).findCapabilitySetIdsByIdIn(capabilityIdsSet);
    }

    @Test
    void positive_emptyInput() {
      capabilitySetService.checkIds(emptyList());
      verifyNoInteractions(capabilitySetRepository);
    }

    @Test
    void negative_capabilitySetNotFoundById() {
      when(capabilitySetRepository.findCapabilitySetIdsByIdIn(Set.of(CAPABILITY_SET_ID))).thenReturn(emptySet());

      var capabilityIds = List.of(CAPABILITY_SET_ID);
      assertThatThrownBy(() -> capabilitySetService.checkIds(capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Capability sets not found by ids: %s", List.of(CAPABILITY_SET_ID));
    }
  }
}
