package org.folio.roles.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.spring.data.OffsetRequest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class BaseCqlJpaRepositoryTest {

  private static final OffsetRequest OFFSET_REQUEST = OffsetRequest.of(0, 100);
  @Spy private BaseCqlJpaRepository<String, UUID> repository;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(repository);
  }

  @Test
  void search_positive_queryIsNull() {
    when(repository.findAll(OFFSET_REQUEST)).thenReturn(new PageImpl<>(List.of("sample-string")));
    var result = repository.findByQuery(null, OFFSET_REQUEST);
    assertThat(result).containsExactly("sample-string");
    verify(repository).findByQuery(null, OFFSET_REQUEST);
  }

  @Test
  void search_positive_queryIsNotNull() {
    var query = "cql.allRecords=1";
    when(repository.findByCql(query, OFFSET_REQUEST)).thenReturn(new PageImpl<>(List.of("sample-string")));
    var result = repository.findByQuery(query, OFFSET_REQUEST);
    assertThat(result).containsExactly("sample-string");
    verify(repository).findByQuery(query, OFFSET_REQUEST);
  }
}
