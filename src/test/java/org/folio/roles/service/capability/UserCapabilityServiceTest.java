package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.entity.UserCapabilityEntity.DEFAULT_USER_CAPABILITY_SORT;
import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.domain.model.PageResult.empty;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.KeycloakUserUtils.keycloakUser;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilityUtils.userCapability;
import static org.folio.roles.support.UserCapabilityUtils.userCapabilityEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.entity.key.UserCapabilityKey;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.mapper.entity.UserCapabilityEntityMapper;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.service.permission.UserPermissionService;
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
class UserCapabilityServiceTest {

  @InjectMocks private UserCapabilityService userCapabilityService;

  @Mock private CapabilityService capabilityService;
  @Mock private KeycloakUserService keycloakUserService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private UserPermissionService userPermissionService;
  @Mock private UserCapabilityRepository userCapabilityRepository;
  @Mock private CapabilityEndpointService capabilityEndpointService;
  @Mock private UserCapabilityEntityMapper userCapabilityEntityMapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_emptyQuery() {
      var offset = OffsetRequest.of(0, 10, DEFAULT_USER_CAPABILITY_SORT);
      var entity = userCapabilityEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedUserCapability = userCapability();

      when(userCapabilityRepository.findByQuery(null, offset)).thenReturn(page);
      when(userCapabilityEntityMapper.convert(entity)).thenReturn(expectedUserCapability);

      var result = userCapabilityService.find(null, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedUserCapability));
    }

    @Test
    void positive_withQuery() {
      var query = "cql.allRecords=1";
      var offset = OffsetRequest.of(0, 10, DEFAULT_USER_CAPABILITY_SORT);
      var entity = userCapabilityEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedUserCapability = userCapability();

      when(userCapabilityRepository.findByQuery(query, offset)).thenReturn(page);
      when(userCapabilityEntityMapper.convert(entity)).thenReturn(expectedUserCapability);

      var result = userCapabilityService.find(query, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedUserCapability));
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();
    private final UUID capabilityId3 = UUID.randomUUID();

    @Test
    void positive_plainCapabilities() {
      var userCapability1 = userCapability(capabilityId1);
      var userCapability2 = userCapability(capabilityId2);
      var capabilityIds = List.of(capabilityId1, capabilityId2);
      var userCapabilityEntity1 = userCapabilityEntity(capabilityId1);
      var userCapabilityEntity2 = userCapabilityEntity(capabilityId2);
      var entities = List.of(userCapabilityEntity1, userCapabilityEntity2);
      var endpoints = List.of(endpoint("/c1", GET), endpoint("/c2", GET));

      doNothing().when(userPermissionService).createPermissions(USER_ID, endpoints);
      when(userCapabilityRepository.saveAll(entities)).thenReturn(entities);
      when(userCapabilityRepository.findUserCapabilities(USER_ID, capabilityIds)).thenReturn(emptyList());
      when(userCapabilityEntityMapper.convert(userCapabilityEntity1)).thenReturn(userCapability1);
      when(userCapabilityEntityMapper.convert(userCapabilityEntity2)).thenReturn(userCapability2);
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(capabilitySetService.findByUserId(USER_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(capabilityEndpointService.getByCapabilityIds(capabilityIds, emptyList())).thenReturn(endpoints);

      var result = userCapabilityService.create(USER_ID, capabilityIds);

      assertThat(result).isEqualTo(asSinglePage(userCapability1, userCapability2));
      verify(capabilityService).checkIds(capabilityIds);
    }

    @Test
    void positive_capabilitySetAssigned() {
      var userCapability1 = userCapability(capabilityId1);
      var userCapability2 = userCapability(capabilityId2);
      var capabilityIds = List.of(capabilityId1, capabilityId2);
      var userCapabilityEntity1 = userCapabilityEntity(capabilityId1);
      var userCapabilityEntity2 = userCapabilityEntity(capabilityId2);
      var entities = List.of(userCapabilityEntity1, userCapabilityEntity2);
      var endpoints = List.of(endpoint("/c1", GET));
      var assignedCapabilityIds = List.of(capabilityId2, capabilityId3);
      var capabilitySet = capabilitySet(assignedCapabilityIds);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      doNothing().when(userPermissionService).createPermissions(USER_ID, endpoints);
      when(userCapabilityRepository.saveAll(entities)).thenReturn(entities);
      when(userCapabilityRepository.findUserCapabilities(USER_ID, capabilityIds)).thenReturn(emptyList());
      when(userCapabilityEntityMapper.convert(userCapabilityEntity1)).thenReturn(userCapability1);
      when(userCapabilityEntityMapper.convert(userCapabilityEntity2)).thenReturn(userCapability2);
      when(capabilitySetService.findByUserId(USER_ID, MAX_VALUE, 0)).thenReturn(asSinglePage(capabilitySet));
      when(capabilityEndpointService.getByCapabilityIds(capabilityIds, assignedCapabilityIds)).thenReturn(endpoints);

      var result = userCapabilityService.create(USER_ID, capabilityIds);

      assertThat(result).isEqualTo(asSinglePage(userCapability1, userCapability2));
      verify(capabilityService).checkIds(capabilityIds);
    }

    @Test
    void negative_emptyCapabilities() {
      var capabilityIds = Collections.<UUID>emptyList();
      assertThatThrownBy(() -> userCapabilityService.create(USER_ID, capabilityIds))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Capability id list is empty");
    }

    @Test
    void negative_existingAssignment() {
      var capIds = List.of(capabilityId1, capabilityId2);
      var userCapabilityEntity = userCapabilityEntity(USER_ID, capabilityId1);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilityRepository.findUserCapabilities(USER_ID, capIds)).thenReturn(List.of(userCapabilityEntity));

      assertThatThrownBy(() -> userCapabilityService.create(USER_ID, capIds))
        .isInstanceOf(EntityExistsException.class)
        .hasMessage("Relation already exists for user='%s' and capabilities=[%s]", USER_ID, capabilityId1);
    }

    @Test
    void negative_userIsNotFound() {
      var errorMessage = "User is not found by id: " + USER_ID;
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      var capabilityIds = List.of(capabilityId1);
      assertThatThrownBy(() -> userCapabilityService.create(USER_ID, capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }
  }

  @Nested
  @DisplayName("deleteAll")
  class DeleteAll {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();
    private final UUID capabilityId3 = UUID.randomUUID();

    @Test
    void positive_plainCapabilities() {
      var expectedEntities = List.of(userCapabilityEntity(USER_ID, capabilityId1));
      var endpoints = List.of(endpoint("/c1", GET));
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(expectedEntities);
      when(capabilitySetService.findByUserId(USER_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(capabilityEndpointService.getByCapabilityIds(List.of(capabilityId1), emptyList())).thenReturn(endpoints);

      userCapabilityService.deleteAll(USER_ID);

      verify(userCapabilityRepository).deleteUserCapabilities(USER_ID, List.of(capabilityId1));
      verify(userPermissionService).deletePermissions(USER_ID, endpoints);
    }

    @Test
    void positive_capabilitySetAssigned() {
      var expectedEntities = List.of(userCapabilityEntity(capabilityId1), userCapabilityEntity(capabilityId2));
      var endpoints = List.of(endpoint("/c2", GET));
      var assignedIds = List.of(capabilityId1, capabilityId3);
      var deprecatedIds = List.of(capabilityId1, capabilityId2);
      var capabilitySet = capabilitySet(assignedIds);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(expectedEntities);
      when(capabilitySetService.findByUserId(USER_ID, MAX_VALUE, 0)).thenReturn(asSinglePage(capabilitySet));
      when(capabilityEndpointService.getByCapabilityIds(deprecatedIds, assignedIds)).thenReturn(endpoints);

      userCapabilityService.deleteAll(USER_ID);

      verify(userCapabilityRepository).deleteUserCapabilities(USER_ID, deprecatedIds);
      verify(userPermissionService).deletePermissions(USER_ID, endpoints);
    }

    @Test
    void negative_userIsNotFound() {
      var errorMessage = "User capabilities are not found for user: " + USER_ID;
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      assertThatThrownBy(() -> userCapabilityService.deleteAll(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }

    @Test
    void negative_userCapabilitiesAreNotFound() {
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(emptyList());
      assertThatThrownBy(() -> userCapabilityService.deleteAll(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Relations between user and capabilities are not found for user: %s", USER_ID);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    void positive() {
      var existingEntity = userCapabilityEntity();
      var existingEntities = List.of(existingEntity);
      var capabilitySetIds = List.of(CAPABILITY_ID);
      var endpoints = List.of(endpoint());
      var entityKey = UserCapabilityKey.of(USER_ID, CAPABILITY_ID);

      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(existingEntities);
      when(userCapabilityRepository.findById(entityKey)).thenReturn(Optional.of(existingEntity));
      when(capabilitySetService.findByUserId(USER_ID, MAX_VALUE, 0)).thenReturn(empty());
      when(capabilityEndpointService.getByCapabilityIds(capabilitySetIds, emptyList())).thenReturn(endpoints);

      userCapabilityService.delete(USER_ID, CAPABILITY_ID);

      verify(userPermissionService).deletePermissions(USER_ID, endpoints);
      verify(userCapabilityRepository).deleteUserCapabilities(USER_ID, capabilitySetIds);
    }

    @Test
    void positive_entityNotFound() {
      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(emptyList());
      userCapabilityService.delete(USER_ID, CAPABILITY_ID);
      verifyNoInteractions(userPermissionService);
      verify(userCapabilityRepository, never()).deleteUserCapabilities(any(), anyList());
    }

    @Test
    void positive_entityNotFoundById() {
      var existingEntity = userCapabilityEntity();
      var existingEntities = List.of(existingEntity);
      var entityKey = UserCapabilityKey.of(USER_ID, CAPABILITY_ID);

      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(existingEntities);
      when(userCapabilityRepository.findById(entityKey)).thenReturn(Optional.empty());

      userCapabilityService.delete(USER_ID, CAPABILITY_ID);

      verifyNoInteractions(userPermissionService);
      verify(userCapabilityRepository, never()).deleteUserCapabilities(any(), anyList());
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private final UUID capabilityId1 = UUID.randomUUID();
    private final UUID capabilityId2 = UUID.randomUUID();
    private final UUID capabilityId3 = UUID.randomUUID();

    @Test
    void positive() {
      var uce1 = userCapabilityEntity(capabilityId1);
      var uce2 = userCapabilityEntity(capabilityId2);
      var uce3 = userCapabilityEntity(capabilityId3);
      var existingEntities = List.of(uce1, uce3);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(capabilitySetService.findByUserId(USER_ID, MAX_VALUE, 0)).thenReturn(PageResult.empty());
      when(userCapabilityRepository.findAllByUserId(USER_ID)).thenReturn(existingEntities);
      when(userCapabilityEntityMapper.convert(uce2)).thenReturn(userCapability(USER_ID, capabilityId2));
      when(userCapabilityEntityMapper.convert(uce3)).thenReturn(userCapability(USER_ID, capabilityId3));
      when(userCapabilityRepository.saveAll(List.of(uce2, uce3))).thenReturn(List.of(uce2, uce3));

      var newIds = List.of(capabilityId2, capabilityId3);
      var deprecatedIds = List.of(capabilityId1);
      var endpointsToAssign = List.of(endpoint("/c2", GET));
      var endpointsToDelete = List.of(endpoint("/c1", GET));
      when(capabilityEndpointService.getByCapabilityIds(eq(newIds), anyList())).thenReturn(endpointsToAssign);
      when(capabilityEndpointService.getByCapabilityIds(eq(deprecatedIds), anyList())).thenReturn(endpointsToDelete);

      var requestCapabilityIds = List.of(capabilityId2, capabilityId3);
      userCapabilityService.update(USER_ID, requestCapabilityIds);

      verify(capabilityService).checkIds(newIds);
      verify(userPermissionService).createPermissions(USER_ID, endpointsToAssign);
      verify(userPermissionService).deletePermissions(USER_ID, endpointsToDelete);
      verify(userCapabilityRepository).deleteUserCapabilities(USER_ID, deprecatedIds);
      verify(capabilityEndpointService).getByCapabilityIds(newIds, List.of(capabilityId1));
      verify(capabilityEndpointService).getByCapabilityIds(deprecatedIds, List.of(capabilityId2, capabilityId3));
    }

    @Test
    void negative_userIsNotFound() {
      var errorMessage = "User is not found by id: " + USER_ID;
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenThrow(new EntityNotFoundException(errorMessage));

      var capabilityIds = List.of(capabilityId1);
      assertThatThrownBy(() -> userCapabilityService.update(USER_ID, capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("User is not found by id: %s", USER_ID);
    }
  }
}
