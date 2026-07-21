package org.folio.roles.integration.kafka.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import org.folio.spring.exception.LiquibaseMigrationException;
import org.folio.test.types.UnitTest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.transaction.TransactionSystemException;
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
  void getBackOff_positive_liquibaseMigrationExceptionIsRetryable() throws Exception {
    // given
    var retryDelay = Duration.ofMillis(500);
    var retryAttempts = 10L;
    var retryConfig = createRetryConfiguration(retryDelay, retryAttempts);
    var kafkaConfig = new KafkaConfiguration(new KafkaProperties(), retryConfig, null);
    var exception = new LiquibaseMigrationException("Migration in progress for tenant: test");

    // when
    var backOff = invokeGetBackOff(kafkaConfig, exception);

    // then
    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    var fixedBackOff = (FixedBackOff) backOff;
    assertThat(fixedBackOff.getInterval()).isEqualTo(retryDelay.toMillis());
    assertThat(fixedBackOff.getMaxAttempts()).isEqualTo(retryAttempts);
  }

  @Test
  void getBackOff_positive_uniqueConstraintViolationIsRetryable() throws Exception {
    // given
    var retryDelay = Duration.ofMillis(500);
    var retryAttempts = 10L;
    var retryConfig = createRetryConfiguration(retryDelay, retryAttempts);
    var kafkaConfig = new KafkaConfiguration(new KafkaProperties(), retryConfig, null);
    var psqlException = new PSQLException("ERROR: duplicate key value violates unique constraint "
      + "\"pk_role_capability\"", PSQLState.UNIQUE_VIOLATION);
    var constraintViolation = new ConstraintViolationException("could not execute statement", psqlException,
      "pk_role_capability");
    var exception = new DataIntegrityViolationException("could not execute statement", constraintViolation);

    // when
    var backOff = invokeGetBackOff(kafkaConfig, exception);

    // then
    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    var fixedBackOff = (FixedBackOff) backOff;
    assertThat(fixedBackOff.getInterval()).isEqualTo(retryDelay.toMillis());
    assertThat(fixedBackOff.getMaxAttempts()).isEqualTo(retryAttempts);
  }

  @Test
  void getBackOff_positive_uniqueConstraintViolationAtCommitIsRetryable() throws Exception {
    // given
    var retryDelay = Duration.ofMillis(500);
    var retryAttempts = 10L;
    var retryConfig = createRetryConfiguration(retryDelay, retryAttempts);
    var kafkaConfig = new KafkaConfiguration(new KafkaProperties(), retryConfig, null);
    var psqlException = new PSQLException("ERROR: duplicate key value violates unique constraint "
      + "\"pk_role_capability\"", PSQLState.UNIQUE_VIOLATION);
    var constraintViolation = new ConstraintViolationException("could not execute statement", psqlException,
      "pk_role_capability");
    var persistenceException = new PersistenceException("could not execute statement", constraintViolation);
    var rollbackException = new RollbackException("Error while committing the transaction", persistenceException);
    var exception = new TransactionSystemException("Could not commit JPA transaction", rollbackException);

    // when
    var backOff = invokeGetBackOff(kafkaConfig, exception);

    // then
    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    var fixedBackOff = (FixedBackOff) backOff;
    assertThat(fixedBackOff.getInterval()).isEqualTo(retryDelay.toMillis());
    assertThat(fixedBackOff.getMaxAttempts()).isEqualTo(retryAttempts);
  }

  @Test
  void getBackOff_negative_notNullViolationIsNotRetryable() throws Exception {
    // given
    var retryConfig = createRetryConfiguration(Duration.ofMillis(500), 10L);
    var kafkaConfig = new KafkaConfiguration(new KafkaProperties(), retryConfig, null);
    var sqlException = new SQLException("ERROR: null value in column \"name\" violates not-null constraint", "23502");
    var exception = new DataIntegrityViolationException("could not execute statement", sqlException);

    // when
    var backOff = invokeGetBackOff(kafkaConfig, exception);

    // then
    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    assertThat(((FixedBackOff) backOff).getMaxAttempts()).isZero();
  }

  @Test
  void getBackOff_negative_noSqlExceptionInChain() throws Exception {
    // given
    var retryConfig = createRetryConfiguration(Duration.ofMillis(500), 10L);
    var kafkaConfig = new KafkaConfiguration(new KafkaProperties(), retryConfig, null);
    var exception = new IllegalStateException("Unexpected error");

    // when
    var backOff = invokeGetBackOff(kafkaConfig, exception);

    // then
    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    assertThat(((FixedBackOff) backOff).getMaxAttempts()).isZero();
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
