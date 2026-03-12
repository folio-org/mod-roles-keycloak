package org.folio.roles.integration.kafka.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import org.folio.roles.exception.LiquibaseMigrationInProgressException;
import org.folio.test.types.UnitTest;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

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

  @Test
  void getBackOff_positive_liquibaseMigrationInProgressExceptionIsRetryable() throws Exception {
    // given
    var retryDelay = Duration.ofMillis(500);
    var retryAttempts = 10L;
    var retryConfig = createRetryConfiguration(retryDelay, retryAttempts);
    var kafkaConfig = new KafkaConfiguration(new KafkaProperties(), retryConfig);
    var exception = new LiquibaseMigrationInProgressException("Migration in progress for tenant: test");

    // when
    var backOff = invokeGetBackOff(kafkaConfig, exception);

    // then
    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    var fixedBackOff = (FixedBackOff) backOff;
    assertThat(fixedBackOff.getInterval()).isEqualTo(retryDelay.toMillis());
    assertThat(fixedBackOff.getMaxAttempts()).isEqualTo(retryAttempts);
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

  private CapabilityEventRetryConfiguration createRetryConfiguration(Duration retryDelay, long retryAttempts) {
    var config = new CapabilityEventRetryConfiguration();
    config.setRetryDelay(retryDelay);
    config.setRetryAttempts(retryAttempts);
    return config;
  }

  private BackOff invokeGetBackOff(KafkaConfiguration kafkaConfig, Exception exception) throws Exception {
    var method = KafkaConfiguration.class.getDeclaredMethod("getBackOff", Exception.class);
    method.setAccessible(true);
    return (BackOff) method.invoke(kafkaConfig, exception);
  }
}
