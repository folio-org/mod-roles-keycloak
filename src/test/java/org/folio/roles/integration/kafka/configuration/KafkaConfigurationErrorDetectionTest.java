package org.folio.roles.integration.kafka.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Optional;
import org.folio.test.types.UnitTest;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

@UnitTest
class KafkaConfigurationErrorDetectionTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "ERROR: relation \"capability\" does not exist",
    "ERROR: column \"module_id\" does not exist",
    "ERROR: type \"capability_action\" does not exist",
    "ERROR: constraint \"fk_capability_endpoint\" does not exist",
    "ERROR: index \"idx_capability_name\" does not exist"
  })
  void detectMigrationError_positive_schemaObjectDoesNotExist(String errorMessage) throws Exception {
    var exception = createException(errorMessage);

    var result = invokeDetectMigrationError(exception);

    assertThat(result).isPresent();
    assertThat(result.get()).contains("does not exist");
  }

  @Test
  void detectMigrationError_negative_wrongExceptionType() throws Exception {
    var exception = new IllegalArgumentException("Some other error");

    var result = invokeDetectMigrationError(exception);

    assertThat(result).isEmpty();
  }

  @Test
  void detectMigrationError_negative_duplicateKeyError() throws Exception {
    var exception = createException("ERROR: duplicate key value violates unique constraint");

    var result = invokeDetectMigrationError(exception);

    assertThat(result).isEmpty();
  }

  @Test
  void detectMigrationError_negative_notNullConstraint() throws Exception {
    var exception = createException("ERROR: null value in column \"name\" violates not-null constraint");

    var result = invokeDetectMigrationError(exception);

    assertThat(result).isEmpty();
  }

  @Test
  void detectMigrationError_negative_wrongCauseType() throws Exception {
    var sqlException = new java.sql.SQLException("Some SQL error");
    var exception = new InvalidDataAccessResourceUsageException("DAO Error", sqlException);

    var result = invokeDetectMigrationError(exception);

    assertThat(result).isEmpty();
  }

  @Test
  void detectMigrationError_negative_noCause() throws Exception {
    var exception = new InvalidDataAccessResourceUsageException("DAO Error without cause");

    var result = invokeDetectMigrationError(exception);

    assertThat(result).isEmpty();
  }

  private InvalidDataAccessResourceUsageException createException(String errorMessage) {
    var psqlException = new PSQLException(errorMessage, PSQLState.UNDEFINED_TABLE);
    var sqlGrammarException = new SQLGrammarException("SQL grammar error", psqlException);
    return new InvalidDataAccessResourceUsageException("Data access error", sqlGrammarException);
  }

  private Optional<String> invokeDetectMigrationError(Exception exception) throws Exception {
    var method = KafkaConfiguration.class.getDeclaredMethod("detectMigrationError", Exception.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    var result = (Optional<String>) method.invoke(null, exception);
    return result;
  }
}
