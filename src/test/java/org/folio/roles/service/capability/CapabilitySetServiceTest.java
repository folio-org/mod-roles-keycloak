package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.entity.CapabilitySetEntity.DEFAULT_CAPABILITY_SET_SORT;
import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySetEntity;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID_V2;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.RESOURCE_NAME;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
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
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.mapper.entity.CapabilitySetEntityMapper;
import org.folio.roles.repository.CapabilitySetRepository;
import org.folio.roles.support.TestUtils;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilitySetServiceTest {

  @InjectMocks private CapabilitySetService capabilitySetService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetRepository capabilitySetRepository;
  @Mock private CapabilitySetEntityMapper mapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
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
    void negative_nameIsTakenForNewCapabilitySet() {
      var capabilityToCreate = capabilitySet();

      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(true);
      when(capabilitySetRepository.existsById(CAPABILITY_SET_ID)).thenReturn(false);

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
    void negative_nameIsTakenForExistingCapabilitySet() {
      var capabilitySet = capabilitySet();
      var capabilitySetEntity = capabilitySetEntity();

      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(true);
      when(capabilitySetRepository.existsById(CAPABILITY_SET_ID)).thenReturn(true);
      when(capabilitySetRepository.save(capabilitySetEntity)).thenReturn(capabilitySetEntity);
      when(mapper.convert(capabilitySet)).thenReturn(capabilitySetEntity);
      when(mapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);
      doNothing().when(capabilityService).checkIds(List.of(CAPABILITY_ID));

      var actual = capabilitySetService.create(capabilitySet);

      assertThat(actual).isEqualTo(capabilitySet);
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

      var capabilitySets = List.of(capabilitySet);
      var actual = capabilitySetService.createAll(capabilitySets);

      assertThat(actual).isEqualTo(capabilitySets);
    }

    @Test
    void positive_batchRequest_emptySets() {
      var actual = capabilitySetService.createAll(emptyList());
      assertThat(actual).isEmpty();
    }

    @Test
    void positive_batchRequest_nameIsTaken() {
      var capabilitySets = List.of(capabilitySet());

      when(capabilitySetRepository.existsByName("test_resource.create")).thenReturn(true);
      when(capabilitySetRepository.existsById(CAPABILITY_SET_ID)).thenReturn(false);

      var actual = capabilitySetService.createAll(capabilitySets);

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
      var updatedEntity = capabilitySetEntity(updatedCapabilityIds);
      var updatedCapabilitySet = capabilitySet(updatedCapabilityIds);

      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(mapper.convert(updatedCapabilitySet)).thenReturn(updatedEntity);
      when(capabilitySetRepository.saveAndFlush(updatedEntity)).thenReturn(updatedEntity);

      capabilitySetService.update(CAPABILITY_SET_ID, updatedCapabilitySet);

      verify(capabilityService).checkIds(updatedCapabilityIds);
    }

    @Test
    void positive_setNameIfNull() {
      var foundEntity = capabilitySetEntity(updatedCapabilityIds);
      var updatedEntity = capabilitySetEntity(updatedCapabilityIds);
      var updatedCapabilitySet = capabilitySet(updatedCapabilityIds).name(null);

      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(mapper.convert(updatedCapabilitySet)).thenReturn(updatedEntity);
      when(capabilitySetRepository.saveAndFlush(updatedEntity)).thenReturn(updatedEntity);

      capabilitySetService.update(CAPABILITY_SET_ID, updatedCapabilitySet);

      verify(capabilityService).checkIds(updatedCapabilityIds);
    }

    @Test
    void positive_actionIsChanged() {
      var foundEntity = capabilitySetEntity(RESOURCE_NAME, CapabilityAction.CREATE);
      var updatedEntity = capabilitySetEntity(RESOURCE_NAME, CapabilityAction.EDIT);
      var updatedCapabilitySet = capabilitySet(RESOURCE_NAME, CapabilityAction.EDIT);

      when(capabilitySetRepository.getReferenceById(CAPABILITY_SET_ID)).thenReturn(foundEntity);
      when(capabilitySetRepository.existsByName("test_resource.edit")).thenReturn(false);
      when(mapper.convert(updatedCapabilitySet)).thenReturn(updatedEntity);
      when(capabilitySetRepository.saveAndFlush(updatedEntity)).thenReturn(updatedEntity);

      capabilitySetService.update(CAPABILITY_SET_ID, updatedCapabilitySet);

      verify(capabilityService).checkIds(List.of(CAPABILITY_ID));
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
    void positive_includeDummyIsFalse() {
      var userCapabilitySetEntity = capabilitySetEntity();
      var pageResult = new PageImpl<>(List.of(userCapabilitySetEntity));
      var expectedCapabilitySet = capabilitySet();

      when(capabilitySetRepository.findByUserId(USER_ID, offsetRequest)).thenReturn(pageResult);
      when(mapper.convert(userCapabilitySetEntity)).thenReturn(expectedCapabilitySet);

      var result = capabilitySetService.findByUserId(USER_ID, false, 100, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedCapabilitySet));
      verify(capabilitySetRepository).findByUserId(USER_ID, offsetRequest);
    }

    @Test
    void positive_includeDummyIsTrue() {
      var userCapabilitySetEntity = capabilitySetEntity();
      var pageResult = new PageImpl<>(List.of(userCapabilitySetEntity));
      var expectedCapabilitySet = capabilitySet();

      when(capabilitySetRepository.findByUserIdIncludeDummy(USER_ID, offsetRequest)).thenReturn(pageResult);
      when(mapper.convert(userCapabilitySetEntity)).thenReturn(expectedCapabilitySet);

      var result = capabilitySetService.findByUserId(USER_ID, true, 100, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedCapabilitySet));
      verify(capabilitySetRepository).findByUserIdIncludeDummy(USER_ID, offsetRequest);
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

      when(capabilitySetRepository.findById(CAPABILITY_SET_ID)).thenReturn(Optional.of(entity));

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
  @DisplayName("findByName")
  class FindByName {

    @Test
    void positive() {
      var capabilityName = "test_resource.view";
      var capabilitySet = capabilitySet();
      var capabilitySetEntity = capabilitySetEntity();

      when(capabilitySetRepository.findByName(capabilityName)).thenReturn(Optional.of(capabilitySetEntity));
      when(mapper.convert(capabilitySetEntity)).thenReturn(capabilitySet);

      var result = capabilitySetService.findByName(capabilityName);

      assertThat(result).isEqualTo(Optional.of(capabilitySet));
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

  @Nested
  @DisplayName("updateApplicationVersion")
  class UpdateApplicationVersion {

    @Test
    void positive() {
      var moduleId = "mod-test-1.0.0";
      capabilitySetService.updateApplicationVersion(moduleId, APPLICATION_ID_V2, APPLICATION_ID);
      verify(capabilitySetRepository).updateApplicationVersion(moduleId, APPLICATION_ID_V2, APPLICATION_ID);
    }
  }
}
