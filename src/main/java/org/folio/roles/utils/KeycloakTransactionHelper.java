package org.folio.roles.utils;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeycloakTransactionHelper {

  /**
   * Executes a Keycloak action, then a DB action. If the DB action fails,
   * executes a compensating Keycloak action.
   *
   * @param keycloakAction     The primary Keycloak action (e.g., create
   *                           permissions)
   * @param databaseAction     The primary database action (e.g., save entities)
   *                           returning a value
   * @param compensationAction The compensating Keycloak action (e.g., delete
   *                           permissions) to run if DB fails
   * @param <T>                The return type of the database action
   * @return The result of the database action
   */
  public static <T> T executeWithCompensation(
    Runnable keycloakAction,
    Supplier<T> databaseAction,
    Runnable compensationAction) {

    keycloakAction.run();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager
        .registerSynchronization(new CompensationSynchronization(compensationAction));
    }

    try {
      return databaseAction.get();
    } catch (Exception dbException) {
      handleFallbackCompensation(dbException, compensationAction);
      if (dbException instanceof RuntimeException) {
        throw (RuntimeException) dbException;
      }
      throw new RuntimeException("Database action failed", dbException);
    }
  }

  /**
   * Executes a Keycloak action, then a DB action without a return value.
   *
   * @param keycloakAction     The primary Keycloak action (e.g., create
   *                           permissions)
   * @param databaseAction     The primary database action (e.g., delete entities)
   * @param compensationAction The compensating Keycloak action (e.g., create
   *                           permissions) to run if DB fails
   */
  public static void executeWithCompensation(
    Runnable keycloakAction,
    Runnable databaseAction,
    Runnable compensationAction) {
    executeWithCompensation(keycloakAction, () -> {
      databaseAction.run();
      return null;
    }, compensationAction);
  }

  private static void handleFallbackCompensation(Exception dbException, Runnable compensationAction) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      log.warn("Database operation failed outside of an active transaction. "
        + "Attempting to execute Keycloak compensation action.", dbException);
      try {
        compensationAction.run();
      } catch (Exception compensationException) {
        log.error("CRITICAL: Keycloak compensation action failed after database operation failure! "
          + "System may be in an inconsistent state.", compensationException);
        dbException.addSuppressed(compensationException);
      }
    }
  }

  private static class CompensationSynchronization implements TransactionSynchronization {
    private final Runnable compensationAction;

    CompensationSynchronization(Runnable compensationAction) {
      this.compensationAction = compensationAction;
    }

    @Override
    public void afterCompletion(int status) {
      if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
        try {
          log.warn("Transaction rolled back. Executing Keycloak compensation action.");
          compensationAction.run();
        } catch (Exception compensationException) {
          log.error("CRITICAL: Keycloak compensation action failed "
              + "during transaction rollback! System may be in an inconsistent state.",
            compensationException);
        }
      }
    }
  }
}
