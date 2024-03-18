package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.entity.CapabilityEntity.DEFAULT_CAPABILITY_SORT;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.PERMISSION_NAME;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.CapabilityUtils.capabilityEntity;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.mapper.entity.CapabilityEntityMapper;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.roles.support.TestUtils;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityServiceTest {

  @InjectMocks private CapabilityService capabilityService;
  @Mock private CapabilityRepository capabilityRepository;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilityEntityMapper capabilityEntityMapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("createSafe")
  class CreateSafe {

    @Test
    void positive() {
      var capability = capability().id(null);
      var capabilityEntity = capabilityEntity();
      when(capabilityRepository.findAllByNames(Set.of("test_resource.create"))).thenReturn(emptyList());
      when(capabilityEntityMapper.convert(capability)).thenReturn(capabilityEntity);

      capabilityService.createSafe(APPLICATION_ID, List.of(capability));

      verify(capabilityRepository).saveAll(List.of(capabilityEntity));
    }

    @Test
    void positive_capabilityByNameExists() {
      var capability = capability().id(null);
      var capabilityEntity = capabilityEntity();
      var capabilityNames = Set.of("test_resource.create");

      when(capabilityRepository.findAllByNames(capabilityNames)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(capability())).thenReturn(capabilityEntity);

      capabilityService.createSafe(APPLICATION_ID, List.of(capability));

      verify(capabilityRepository).saveAll(List.of(capabilityEntity));
    }

    @Test
    void positive_emptyList() {
      capabilityService.createSafe(APPLICATION_ID, emptyList());
      Mockito.verifyNoInteractions(capabilityRepository);
    }
  }

  @Nested
  @DisplayName("get")
  class Get {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      when(capabilityRepository.getReferenceById(CAPABILITY_ID)).thenReturn(capabilityEntity);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.get(CAPABILITY_ID);

      assertThat(result).isEqualTo(capability);
    }

    @Test
    void negative_entityNotFound() {
      when(capabilityRepository.getReferenceById(CAPABILITY_ID)).thenThrow(EntityNotFoundException.class);
      assertThatThrownBy(() -> capabilityService.get(CAPABILITY_ID))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive() {
      var query = "cql.allRecords = 1";
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByQuery(query, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.find(query, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }

    @Test
    void positive_noQuery() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByQuery(null, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.find(null, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive_expandIsFalse() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByUserId(USER_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByUserId(USER_ID, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }

    @Test
    void positive_expandIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findAllByUserId(USER_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByUserId(USER_ID, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }
  }

  @Nested
  @DisplayName("findByRoleId")
  class FindByRoleId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive_expandIsFalse() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findByRoleId(ROLE_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByRoleId(ROLE_ID, false, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }

    @Test
    void positive_expandIsTrue() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityEntityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilityRepository.findAllByRoleId(ROLE_ID, offsetRequest)).thenReturn(capabilityEntityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByRoleId(ROLE_ID, true, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }
  }

  @Nested
  @DisplayName("findByNames")
  class FindByNames {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityNames = List.of("test_resource.create");

      when(capabilityRepository.findAllByNames(capabilityNames)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(List.of(capabilityEntity))).thenReturn(List.of(capability));

      var result = capabilityService.findByNames(capabilityNames);

      assertThat(result).isEqualTo(List.of(capability));
    }
  }

  @Nested
  @DisplayName("findByCapabilitySetId")
  class FindByCapabilitySetId {

    private final OffsetRequest offsetRequest = OffsetRequest.of(0, 10, DEFAULT_CAPABILITY_SORT);

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var entityPage = new PageImpl<>(List.of(capabilityEntity), offsetRequest, 100);

      when(capabilitySetService.get(CAPABILITY_SET_ID)).thenReturn(capabilitySet());
      when(capabilityRepository.findByCapabilitySetId(CAPABILITY_SET_ID, offsetRequest)).thenReturn(entityPage);
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, 10, 0);

      assertThat(result).isEqualTo(PageResult.of(100, List.of(capability)));
    }

    @Test
    void negative_capabilitySetIsNotFound() {
      when(capabilitySetService.get(CAPABILITY_SET_ID)).thenThrow(EntityNotFoundException.class);
      assertThatThrownBy(() -> capabilityService.findByCapabilitySetId(CAPABILITY_SET_ID, 10, 0))
        .isInstanceOf(EntityNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findByCapabilitySetIds")
  class FindByCapabilitySetIds {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntities = List.of(capabilityEntity());
      var capabilitySetIds = List.of(CAPABILITY_SET_ID);

      when(capabilityRepository.findByCapabilitySetIds(capabilitySetIds)).thenReturn(capabilityEntities);
      when(capabilityEntityMapper.convert(capabilityEntities)).thenReturn(List.of(capability));

      var result = capabilityService.findByCapabilitySetIds(capabilitySetIds);

      assertThat(result).containsExactly(capability);
    }
  }

  @Nested
  @DisplayName("checkCapabilityIds")
  class CheckCapabilityIds {

    @Test
    void positive() {
      var capabilityIds = List.of(CAPABILITY_ID);
      var capabilityIdsSet = Set.of(CAPABILITY_ID);
      when(capabilityRepository.findCapabilityIdsByIdIn(capabilityIdsSet)).thenReturn(capabilityIdsSet);

      capabilityService.checkIds(capabilityIds);

      verify(capabilityRepository).findCapabilityIdsByIdIn(capabilityIdsSet);
    }

    @Test
    void positive_emptyInput() {
      capabilityService.checkIds(emptyList());
      verifyNoInteractions(capabilityRepository);
    }

    @Test
    void negative_capabilityNotFoundById() {
      when(capabilityRepository.findCapabilityIdsByIdIn(Set.of(CAPABILITY_ID))).thenReturn(emptySet());

      var capabilityIds = List.of(CAPABILITY_ID);
      assertThatThrownBy(() -> capabilityService.checkIds(capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Capabilities not found by ids: %s", List.of(CAPABILITY_ID));
    }
  }

  @Nested
  @DisplayName("findByIds")
  class FindByIds {

    @Test
    void positive() {
      var capability = capability();
      var capabilityEntity = capabilityEntity();
      var capabilityIds = List.of(CAPABILITY_ID);
      when(capabilityRepository.findAllById(capabilityIds)).thenReturn(List.of(capabilityEntity));
      when(capabilityEntityMapper.convert(capabilityEntity)).thenReturn(capability);

      var result = capabilityService.findByIds(capabilityIds);

      assertThat(result).isEqualTo(List.of(capability));
    }
  }

  @Nested
  @DisplayName("getUserPermissions")
  class GetUserPermissions {

    @Test
    void positive() {
      when(capabilityRepository.findAllFolioPermissions(USER_ID)).thenReturn(List.of(PERMISSION_NAME));
      var result = capabilityService.getUserPermissions(USER_ID, false);

      assertThat(result).containsExactly(PERMISSION_NAME);
    }

    @Test
    void positiveOnlyVisiblePermissions() {
      var prefixes = ArgumentCaptor.forClass(String.class);
      var permissions = List.of(PERMISSION_NAME);
      when(capabilityRepository.findVisibleFolioPermissions(eq(USER_ID), prefixes.capture())).thenReturn(permissions);
      var result = capabilityService.getUserPermissions(USER_ID, true);

      assertThat(result).containsExactly(PERMISSION_NAME);
      assertThat(prefixes.getValue()).isEqualTo("{ui-, module, plugin}");
    }
  }
}
