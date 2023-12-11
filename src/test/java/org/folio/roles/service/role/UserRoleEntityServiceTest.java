package org.folio.roles.service.role;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.UserRoleTestUtils.userRole;
import static org.folio.roles.support.UserRoleTestUtils.userRoles;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Metadata;
import org.folio.roles.domain.entity.UserRoleEntity;
import org.folio.roles.mapper.entity.DateConvertHelper;
import org.folio.roles.mapper.entity.UserRoleMapper;
import org.folio.roles.mapper.entity.UserRoleMapperImpl;
import org.folio.roles.repository.UserRoleRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserRoleEntityServiceTest {

  private static final UUID USER_ID = fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ROLE_ID = fromString("00000000-0000-0000-0000-000000000002");
  public static final List<UUID> ROLE_IDS = singletonList(ROLE_ID);

  @Mock private UserRoleRepository repository;
  @Spy private UserRoleMapper mapper = new UserRoleMapperImpl(new DateConvertHelper()) {};

  @InjectMocks private UserRoleEntityService service;

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(repository);
  }

  private static UserRoleEntity userRoleEntity() {
    var entity = new UserRoleEntity();
    entity.setUserId(USER_ID);
    entity.setRoleId(ROLE_ID);
    return entity;
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      when(repository.findByUserIdAndRoleIdIn(USER_ID, ROLE_IDS)).thenReturn(emptyList());
      when(repository.saveAll(anyList())).thenReturn(List.of(userRoleEntity()));

      var result = service.create(USER_ID, ROLE_IDS);

      assertThat(result).containsExactly(userRole(USER_ID, ROLE_ID).metadata(new Metadata()));
      verify(mapper).toEntity(List.of(userRole(USER_ID, ROLE_ID)));
      verify(mapper).toDto(List.of(userRoleEntity()));
    }

    @Test
    void negative_userRoleAlreadyExists() {
      when(repository.findByUserIdAndRoleIdIn(USER_ID, ROLE_IDS)).thenReturn(List.of(userRoleEntity()));
      assertThatThrownBy(() -> service.create(USER_ID, ROLE_IDS))
        .isInstanceOf(EntityExistsException.class)
        .hasMessageMatching("Relations between user and roles already exists \\(userId: .*, roles: \\[.*]\\)");
    }

    @Test
    void negative_repositoryThrowsException() {
      when(repository.findByUserIdAndRoleIdIn(USER_ID, ROLE_IDS)).thenReturn(emptyList());
      when(repository.saveAll(anyList())).thenThrow(RuntimeException.class);
      assertThatThrownBy(() -> service.create(USER_ID, ROLE_IDS)).isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("deleteByUserId")
  class DeleteByUserId {

    @Test
    void positive() {
      when(repository.findByUserId(USER_ID)).thenReturn(List.of(userRoleEntity()));
      service.deleteByUserId(USER_ID);
      verify(repository).deleteByUserId(USER_ID);
    }

    @Test
    void negative_noRolesAssigned() {
      when(repository.findByUserId(USER_ID)).thenReturn(emptyList());
      assertThatThrownBy(() -> service.deleteByUserId(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("There are no assigned roles for userId: " + USER_ID);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    void positive() {
      service.delete(USER_ID, ROLE_IDS);
      verify(repository).deleteByUserIdAndRoleIdIn(USER_ID, ROLE_IDS);
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    @Test
    void positive() {
      var rolesUserEntity = userRoleEntity();
      when(repository.findByUserId(USER_ID)).thenReturn(List.of(rolesUserEntity));

      var result = service.findByUserId(USER_ID);

      var expectedUserRole = userRole(USER_ID, ROLE_ID).metadata(new Metadata());
      assertThat(result).containsExactly(expectedUserRole);
    }

    @Test
    void negative_entityNotFound() {
      when(repository.findByUserId(any(UUID.class))).thenReturn(emptyList());
      var result = service.findByUserId(USER_ID);
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByQuery")
  class FindByQuery {

    @Test
    void positive_queryNotExists() {
      when(repository.findAll(OffsetRequest.of(10, 10))).thenReturn(Page.empty());
      service.findByQuery(null, 10, 10);
      verify(repository, never()).findByCql(anyString(), any(OffsetRequest.class));
    }

    @Test
    void positive_queryExists() {
      when(repository.findByCql(anyString(), any(OffsetRequest.class))).thenReturn(Page.empty());

      var query = "userId=" + USER_ID;
      service.findByQuery(query, 10, 10);

      verify(repository, never()).findAll(any(OffsetRequest.class));
    }

    @Test
    void positive() {
      var rolesUserEntity = List.of(userRoleEntity());
      var expectedPage = new PageImpl<>(rolesUserEntity, Pageable.ofSize(1), 1);
      var offset = 0;
      var limit = 10;
      var cqlQuery = "cql.allRecords = 1";

      when(repository.findByCql(cqlQuery, OffsetRequest.of(offset, limit))).thenReturn(expectedPage);

      var result = service.findByQuery(cqlQuery, offset, limit);

      var expectedUserRole = userRole(USER_ID, ROLE_ID).metadata(new Metadata());
      assertThat(result).isEqualTo(userRoles(expectedUserRole));
    }
  }
}
