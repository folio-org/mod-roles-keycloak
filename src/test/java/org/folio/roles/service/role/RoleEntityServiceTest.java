package org.folio.roles.service.role;

import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Metadata;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.entity.RoleEntity;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.folio.roles.mapper.entity.DateConvertHelper;
import org.folio.roles.mapper.entity.RoleEntityMapper;
import org.folio.roles.mapper.entity.RoleEntityMapperImpl;
import org.folio.roles.repository.RoleEntityRepository;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RoleEntityServiceTest {

  private static final UUID ROLE_ID = fromString("00000000-0000-0000-0000-000000000001");
  private static final String ROLE_NAME = "test-role-name";
  private static final String ROLE_DESCRIPTION = "test-role-description";

  @Spy private RoleEntityMapper mapper = new RoleEntityMapperImpl(new DateConvertHelper());
  @Mock private RoleEntityRepository repository;

  @InjectMocks private RoleEntityService service;

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(repository);
  }

  private static Role createRoleDto() {
    return new Role()
      .name(ROLE_NAME)
      .id(ROLE_ID)
      .description(ROLE_DESCRIPTION);
  }

  private static RoleEntity createRoleEntity() {
    var entity = new RoleEntity();
    entity.setName(ROLE_NAME);
    entity.setId(ROLE_ID);
    entity.setDescription(ROLE_DESCRIPTION);
    entity.setType(EntityRoleType.REGULAR);
    return entity;
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var entity = createRoleEntity();
      var dto = createRoleDto();

      when(repository.save(any(RoleEntity.class))).thenReturn(entity);

      var role = service.create(dto);

      assertEquals(role.getId(), dto.getId());
      assertEquals(role.getName(), dto.getName());
      assertEquals(role.getDescription(), dto.getDescription());
    }

    @Test
    void negative_repositoryThrowsException() {
      when(repository.save(any(RoleEntity.class))).thenThrow(RuntimeException.class);

      var dto = createRoleDto();
      assertThrows(RuntimeException.class, () -> service.create(dto));
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var role = createRoleDto();
      var roleEntity = createRoleEntity();
      var updatedDescription = "updated";
      role.setDescription(updatedDescription);
      var updatedEntity = mapper.toRoleEntity(role);

      when(repository.getReferenceById(role.getId())).thenReturn(roleEntity);
      when(repository.save(any(RoleEntity.class))).thenReturn(updatedEntity);

      var updated = service.update(role);

      assertEquals(ROLE_ID, updated.getId());
      assertEquals(ROLE_NAME, updated.getName());
      assertEquals(updatedDescription, updated.getDescription());
    }

    @Test
    void negative_entityNotFound() {
      var role = createRoleDto();

      when(repository.getReferenceById(role.getId())).thenThrow(EntityNotFoundException.class);

      assertThrows(EntityNotFoundException.class, () -> service.update(role));
    }

    @Test
    void negative_userIdIsNull() {
      var role = createRoleDto();
      role.setId(null);

      assertThrowsExactly(NullPointerException.class, () -> service.update(role),
        "To update roles user ID cannot be null");
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      doNothing().when(repository).deleteById(ROLE_ID);

      service.deleteById(ROLE_ID);

      verify(repository).deleteById(ROLE_ID);
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    @Test
    void positive() {
      var roleEntity = createRoleEntity();

      when(repository.getReferenceById(ROLE_ID)).thenReturn(roleEntity);

      var role = service.getById(ROLE_ID);

      assertEquals(ROLE_ID, role.getId());
      assertEquals(ROLE_NAME, role.getName());
      assertEquals(ROLE_DESCRIPTION, role.getDescription());
    }

    @Test
    void negative_entityNotFound() {
      when(repository.getReferenceById(any(UUID.class))).thenThrow(EntityNotFoundException.class);

      assertThrows(EntityNotFoundException.class, () -> service.getById(ROLE_ID));
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
      var roles = List.of(createRoleEntity());
      var expectedPage = new PageImpl<>(roles, Pageable.ofSize(1), 1);

      when(repository.findByCql(cqlQuery, OffsetRequest.of(offset, limit))).thenReturn(expectedPage);

      var result = service.findByQuery(cqlQuery, offset, limit);

      assertEquals(1, result.size());
      assertEquals(ROLE_ID, result.get(0).getId());
      assertEquals(ROLE_NAME, result.get(0).getName());
      assertEquals(ROLE_DESCRIPTION, result.get(0).getDescription());
    }
  }

  @Nested
  @DisplayName("findByIds")
  class FindByIds {

    @Test
    void positive() {
      var roleIds = List.of(ROLE_ID);
      when(repository.findByIdIn(roleIds)).thenReturn(List.of(createRoleEntity()));

      var result = service.findByIds(roleIds);

      assertThat(result).containsExactly(createRoleDto().metadata(new Metadata()));
    }
  }

  @Nested
  @DisplayName("existById")
  class ExistById {

    @ParameterizedTest
    @DisplayName("positive_parameterized")
    @ValueSource(booleans = {true, false})
    void positive(boolean exists) {
      when(repository.existsById(ROLE_ID)).thenReturn(exists);
      var result = service.existById(ROLE_ID);
      assertThat(result).isEqualTo(exists);
    }
  }
}
