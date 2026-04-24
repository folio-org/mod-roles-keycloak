package org.folio.roles.integration.kafka.configuration;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.folio.integration.kafka.consumer.EnableKafkaConsumer;
import org.folio.integration.kafka.consumer.filter.TenantIsDisabledException;
import org.folio.integration.kafka.consumer.filter.TenantsAreDisabledException;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.spring.exception.LiquibaseMigrationException;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.kafka.KafkaException.Level;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

@Log4j2
@Configuration
@EnableKafkaConsumer
@RequiredArgsConstructor
public class KafkaConfiguration {

  private final KafkaProperties kafkaProperties;
  private final CapabilityEventRetryConfiguration retryConfiguration;

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, ResourceEvent<?>> kafkaListenerContainerFactory(
    ConsumerFactory<String, ResourceEvent<?>> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, ResourceEvent<?>>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(capabilityEventErrorHandler());
    return factory;
  }

  @Bean
  public ConsumerFactory<String, ResourceEvent<?>> jsonNodeConsumerFactory() {
    var deserializer = new JacksonJsonDeserializer<ResourceEvent<?>>(ResourceEvent.class);
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
    config.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
  }

  private DefaultErrorHandler capabilityEventErrorHandler() {
    var errorHandler = new DefaultErrorHandler((message, exception) ->
      log.warn("Failed to process capability event [record: {}]", message, exception.getCause()));
    errorHandler.setBackOffFunction((message, exception) -> getBackOff(exception));
    errorHandler.setLogLevel(Level.DEBUG);

    return errorHandler;
  }

  private BackOff getBackOff(Exception exception) {
    if (exception instanceof LiquibaseMigrationException) {
      log.warn("Liquibase migration in progress, retrying Kafka event", exception);
      return getFixedBackOff();
    }

    if (exception instanceof TenantsAreDisabledException
      || exception instanceof TenantIsDisabledException) {
      log.warn("Tenant(s) is disabled, retrying Kafka event", exception);
      return getFixedBackOff();
    }

    var migrationErrorMessage = detectMigrationError(exception);
    if (migrationErrorMessage.isPresent()) {
      log.warn("Database migration in progress, retrying Kafka event [error: {}]", migrationErrorMessage.get());
      return getFixedBackOff();
    }

    log.warn("Non-retryable error, skipping message", exception);
    return new FixedBackOff(0L, 0L);
  }

  private FixedBackOff getFixedBackOff() {
    return new FixedBackOff(retryConfiguration.getRetryDelay().toMillis(), retryConfiguration.getRetryAttempts());
  }

  /**
   * Detects PostgreSQL errors indicating database migration is in progress. Covers schema objects not yet created
   * (tables, columns, types, constraints, indexes).
   */
  private static Optional<String> detectMigrationError(Exception exception) {
    return Optional.of(exception)
      .filter(InvalidDataAccessResourceUsageException.class::isInstance)
      .map(Throwable::getCause)
      .filter(SQLGrammarException.class::isInstance)
      .map(Throwable::getCause)
      .filter(throwable -> Objects.equals(throwable.getClass().getSimpleName(), "PSQLException"))
      .map(Throwable::getMessage)
      .filter(msg -> msg.startsWith("ERROR:") && msg.contains("does not exist"))
      .map(msg -> msg.replaceAll("\\s+", " "));
  }
}
