package org.folio.roles.repository.generators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Data;
import org.folio.test.types.UnitTest;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class FolioUuidGeneratorImplTest {

  private FolioUuidGeneratorImpl folioUuidGenerator;
  @Mock private EntityPersister entityPersister;
  @Mock private CustomIdGeneratorCreationContext creationContext;
  @Mock private SharedSessionContractImplementor sharedSessionContractImplementor;

  @BeforeEach
  void setUp() throws Exception {
    var idField = TestEntity.class.getDeclaredField("id");
    var annotation = idField.getAnnotation(FolioUuidGenerator.class);
    folioUuidGenerator = new FolioUuidGeneratorImpl(annotation, idField, creationContext);
  }

  @Test
  void generate_positive_entityIdIsNull() {
    var testEntity = new TestEntity();
    when(sharedSessionContractImplementor.getEntityPersister(null, testEntity)).thenReturn(entityPersister);
    when(entityPersister.getIdentifier(testEntity, sharedSessionContractImplementor)).thenReturn(null);

    var result = folioUuidGenerator.generate(sharedSessionContractImplementor, testEntity, null, null);

    assertThat(result).isNotNull();
  }

  @Test
  void generate_positive_entityIdIsNotNull() {
    var testEntity = new TestEntity();
    var entityId = UUID.randomUUID();
    when(sharedSessionContractImplementor.getEntityPersister(null, testEntity)).thenReturn(entityPersister);
    when(entityPersister.getIdentifier(testEntity, sharedSessionContractImplementor)).thenReturn(entityId);

    var result = folioUuidGenerator.generate(sharedSessionContractImplementor, testEntity, null, null);

    assertThat(result).isEqualTo(entityId);
  }

  @Data
  private static final class TestEntity {

    @Id
    @FolioUuidGenerator
    private UUID id;
  }
}
