package org.folio.roles.service.capability;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.entity.UserCapabilitySetEntity.DEFAULT_USER_CAPABILITY_SET_SORT;
import static org.folio.roles.domain.model.PageResult.asSinglePage;
import static org.folio.roles.domain.model.PageResult.empty;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.KeycloakUtils.keycloakUser;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySet;
import static org.folio.roles.support.UserCapabilitySetUtils.userCapabilitySetEntity;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.RequestValidationException;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.mapper.entity.UserCapabilitySetEntityMapper;
import org.folio.roles.repository.UserCapabilitySetRepository;
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
class UserCapabilitySetServiceTest {

  @InjectMocks private UserCapabilitySetService userCapabilitySetService;

  @Mock private CapabilityService capabilityService;
  @Mock private KeycloakUserService keycloakUserService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private UserPermissionService userPermissionService;
  @Mock private CapabilityEndpointService endpointService;
  @Mock private UserCapabilitySetRepository userCapabilitySetRepository;
  @Mock private UserCapabilitySetEntityMapper userCapabilitySetEntityMapper;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_emptyQuery() {
      var offset = OffsetRequest.of(0, 10, DEFAULT_USER_CAPABILITY_SET_SORT);
      var entity = userCapabilitySetEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedUserCapability = userCapabilitySet();

      when(userCapabilitySetRepository.findByQuery(null, offset)).thenReturn(page);
      when(userCapabilitySetEntityMapper.convert(entity)).thenReturn(expectedUserCapability);

      var result = userCapabilitySetService.find(null, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedUserCapability));
    }

    @Test
    void positive_withQuery() {
      var query = "cql.allRecords=1";
      var offset = OffsetRequest.of(0, 10, DEFAULT_USER_CAPABILITY_SET_SORT);
      var entity = userCapabilitySetEntity();
      var page = new PageImpl<>(List.of(entity));
      var expectedUserCapability = userCapabilitySet();

      when(userCapabilitySetRepository.findByQuery(query, offset)).thenReturn(page);
      when(userCapabilitySetEntityMapper.convert(entity)).thenReturn(expectedUserCapability);

      var result = userCapabilitySetService.find(query, 10, 0);

      assertThat(result).isEqualTo(asSinglePage(expectedUserCapability));
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    private final UUID capabilitySetId1 = UUID.randomUUID();
    private final UUID capabilitySetId2 = UUID.randomUUID();

    @Test
    void positive() {
      var userCapability1 = userCapabilitySet(capabilitySetId1);
      var userCapability2 = userCapabilitySet(capabilitySetId2);
      var capabilitySetIds = List.of(capabilitySetId1, capabilitySetId2);
      var userCapabilityEntity1 = userCapabilitySetEntity(capabilitySetId1);
      var userCapabilityEntity2 = userCapabilitySetEntity(capabilitySetId2);
      var entities = List.of(userCapabilityEntity1, userCapabilityEntity2);
      var endpoints = List.of(endpoint());

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilitySetEntityMapper.convert(userCapabilityEntity1)).thenReturn(userCapability1);
      when(userCapabilitySetEntityMapper.convert(userCapabilityEntity2)).thenReturn(userCapability2);
      when(userCapabilitySetRepository.findUserCapabilitySets(USER_ID, capabilitySetIds)).thenReturn(emptyList());
      when(userCapabilitySetRepository.saveAll(entities)).thenReturn(entities);
      when(capabilityService.findByUserId(USER_ID, false, MAX_VALUE, 0)).thenReturn(empty());
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);
      doNothing().when(userPermissionService).createPermissions(USER_ID, endpoints);

      var result = userCapabilitySetService.create(USER_ID, capabilitySetIds);

      assertThat(result).isEqualTo(asSinglePage(userCapability1, userCapability2));
      verify(capabilitySetService).checkIds(capabilitySetIds);
    }

    @Test
    void negative_emptyCapabilities() {
      var capabilityIds = Collections.<UUID>emptyList();
      assertThatThrownBy(() -> userCapabilitySetService.create(USER_ID, capabilityIds))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("List with capability set identifiers is empty");
    }

    @Test
    void negative_existingAssignment() {
      var capIds = List.of(capabilitySetId1, capabilitySetId2);
      var userCapabilityEntity = userCapabilitySetEntity(USER_ID, capabilitySetId1);
      var foundEntities = List.of(userCapabilityEntity);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilitySetRepository.findUserCapabilitySets(USER_ID, capIds)).thenReturn(foundEntities);

      assertThatThrownBy(() -> userCapabilitySetService.create(USER_ID, capIds))
        .isInstanceOf(EntityExistsException.class)
        .hasMessage("Relation already exists for user='%s' and capabilitySets=[%s]", USER_ID, capabilitySetId1);
    }

    @Test
    void negative_userIsNotFound() {
      var errorMessage = "User is not found by id: " + USER_ID;
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      var capabilityIds = List.of(CAPABILITY_SET_ID);
      assertThatThrownBy(() -> userCapabilitySetService.create(USER_ID, capabilityIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }
  }

  @Nested
  @DisplayName("deleteAll")
  class DeleteAll {

    @Test
    void positive() {
      var expectedEntities = List.of(userCapabilitySetEntity());
      var capabilitySetIds = List.of(CAPABILITY_SET_ID);
      var endpoints = List.of(endpoint());

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilitySetRepository.findAllByUserId(USER_ID)).thenReturn(expectedEntities);
      when(capabilityService.findByUserId(USER_ID, false, MAX_VALUE, 0)).thenReturn(empty());
      when(endpointService.getByCapabilitySetIds(capabilitySetIds, emptyList(), emptyList())).thenReturn(endpoints);

      userCapabilitySetService.deleteAll(USER_ID);

      verify(userPermissionService).deletePermissions(USER_ID, endpoints);
      verify(userCapabilitySetRepository).deleteUserCapabilitySets(USER_ID, capabilitySetIds);
    }

    @Test
    void negative_userIsNotFound() {
      var errorMessage = "User is not found by id: " + USER_ID;
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenThrow(new EntityNotFoundException(errorMessage));
      assertThatThrownBy(() -> userCapabilitySetService.deleteAll(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }

    @Test
    void negative_userCapabilitiesAreNotFound() {
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilitySetRepository.findAllByUserId(USER_ID)).thenReturn(emptyList());
      assertThatThrownBy(() -> userCapabilitySetService.deleteAll(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Relations between user and capability sets are not found for user: %s", USER_ID);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private final UUID capabilitySetId1 = UUID.randomUUID();
    private final UUID capabilitySetId2 = UUID.randomUUID();
    private final UUID capabilitySetId3 = UUID.randomUUID();

    @Test
    void positive() {
      var ucse1 = userCapabilitySetEntity(capabilitySetId1);
      var ucse2 = userCapabilitySetEntity(capabilitySetId2);
      var ucse3 = userCapabilitySetEntity(capabilitySetId3);
      var existingEntities = List.of(ucse1, ucse3);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilitySetRepository.findAllByUserId(USER_ID)).thenReturn(existingEntities);
      when(userCapabilitySetEntityMapper.convert(ucse2)).thenReturn(userCapabilitySet(USER_ID, capabilitySetId2));
      when(userCapabilitySetRepository.saveAll(List.of(ucse2))).thenReturn(List.of(ucse2));
      when(capabilityService.findByUserId(USER_ID, false, MAX_VALUE, 0)).thenReturn(PageResult.empty());

      var newIds = List.of(capabilitySetId2);
      var deprecatedIds = List.of(capabilitySetId1);
      var endpointsToAssign = List.of(endpoint("/c2", GET));
      var endpointsToDel = List.of(endpoint("/c1", GET));
      var idsToAssign = List.of(capabilitySetId1, capabilitySetId3);
      var assignedIds = Set.of(capabilitySetId2, capabilitySetId3);

      when(endpointService.getByCapabilitySetIds(newIds, idsToAssign, emptyList())).thenReturn(endpointsToAssign);
      when(endpointService.getByCapabilitySetIds(deprecatedIds, assignedIds, emptyList())).thenReturn(endpointsToDel);

      var requestCapabilityIds = List.of(capabilitySetId2, capabilitySetId3);
      userCapabilitySetService.update(USER_ID, requestCapabilityIds);

      verify(capabilitySetService).checkIds(newIds);
      verify(userPermissionService).createPermissions(USER_ID, endpointsToAssign);
      verify(userPermissionService).deletePermissions(USER_ID, endpointsToDel);
      verify(userCapabilitySetRepository).deleteUserCapabilitySets(USER_ID, deprecatedIds);
      verify(capabilityService, times(2)).findByUserId(USER_ID, false, MAX_VALUE, 0);
    }

    @Test
    void negative_notingToUpdate() {
      var capabilityIds = List.of(CAPABILITY_SET_ID);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(userCapabilitySetRepository.findAllByUserId(USER_ID)).thenReturn(List.of(userCapabilitySetEntity()));

      assertThatThrownBy(() -> userCapabilitySetService.update(USER_ID, capabilityIds))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("Nothing to update, user-capability set relations are not changed");
    }

    @Test
    void negative_userIsNotFound() {
      var errorMessage = "User is not found by id: " + USER_ID;
      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenThrow(new EntityNotFoundException(errorMessage));

      var capabilitySetIds = List.of(capabilitySetId1);
      assertThatThrownBy(() -> userCapabilitySetService.update(USER_ID, capabilitySetIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage(errorMessage);
    }
  }
}
