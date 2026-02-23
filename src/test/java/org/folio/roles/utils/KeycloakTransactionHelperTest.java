package org.folio.roles.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class KeycloakTransactionHelperTest {

  private MockedStatic<TransactionSynchronizationManager> txManagerMock;

  @BeforeEach
  void setUp() {
    txManagerMock = mockStatic(TransactionSynchronizationManager.class);
  }

  @AfterEach
  void tearDown() {
    txManagerMock.close();
  }

  @Test
  void executeWithCompensation_success_withReturnValue() {
    var keycloakAction = mock(Runnable.class);
    @SuppressWarnings("unchecked")
    var databaseAction = (Supplier<String>) mock(Supplier.class);
    var compensationAction = mock(Runnable.class);

    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
    when(databaseAction.get()).thenReturn("success");

    var result = KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
      compensationAction);

    assertThat(result).isEqualTo("success");
    verify(keycloakAction).run();
    verify(databaseAction).get();
    txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
    verifyNoInteractions(compensationAction);
  }

  @Test
  void executeWithCompensation_success_withoutReturnValue() {
    var keycloakAction = mock(Runnable.class);
    var databaseAction = mock(Runnable.class);
    var compensationAction = mock(Runnable.class);

    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);

    KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

    verify(keycloakAction).run();
    verify(databaseAction).run();
    verifyNoInteractions(compensationAction);
  }

  @Test
  void executeWithCompensation_dbFails_noActiveTransaction_compensationSucceeds() {
    var keycloakAction = mock(Runnable.class);
    @SuppressWarnings("unchecked")
    var databaseAction = (Supplier<String>) mock(Supplier.class);
    var compensationAction = mock(Runnable.class);

    var dbException = new RuntimeException("DB failed");
    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
    when(databaseAction.get()).thenThrow(dbException);

    assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
      compensationAction))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("DB failed");

    verify(keycloakAction).run();
    verify(databaseAction).get();
    verify(compensationAction).run();
  }

  @Test
  void executeWithCompensation_dbFails_noActiveTransaction_compensationFails() {
    var keycloakAction = mock(Runnable.class);
    var databaseAction = mock(Runnable.class);
    var compensationAction = mock(Runnable.class);

    var dbException = new RuntimeException("DB failed");
    var compException = new RuntimeException("Compensation failed");

    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
    doThrow(dbException).when(databaseAction).run();
    doThrow(compException).when(compensationAction).run();

    try {
      KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);
    } catch (RuntimeException ex) {
      assertThat(ex.getMessage()).isEqualTo("DB failed");
      assertThat(ex.getSuppressed()).hasSize(1).containsExactly(compException);
    }

    verify(keycloakAction).run();
    verify(databaseAction).run();
    verify(compensationAction).run();
  }

  @Test
  void executeWithCompensation_activeTransaction_rollbackExecutesCompensation() {
    var keycloakAction = mock(Runnable.class);
    @SuppressWarnings("unchecked")
    var databaseAction = (Supplier<String>) mock(Supplier.class);
    var compensationAction = mock(Runnable.class);

    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
    when(databaseAction.get()).thenReturn("success");

    KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

    ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
    txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

    // Simulate rollback
    TransactionSynchronization sync = syncCaptor.getValue();
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    verify(compensationAction).run();
  }

  @Test
  void executeWithCompensation_activeTransaction_commitIgnoresCompensation() {
    var keycloakAction = mock(Runnable.class);
    @SuppressWarnings("unchecked")
    var databaseAction = (Supplier<String>) mock(Supplier.class);
    var compensationAction = mock(Runnable.class);

    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
    when(databaseAction.get()).thenReturn("success");

    KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

    ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
    txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

    // Simulate successful commit
    TransactionSynchronization sync = syncCaptor.getValue();
    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

    verifyNoInteractions(compensationAction);
  }

  @Test
  void executeWithCompensation_activeTransaction_dbFails_throwsException_doesNotCompensateYet() {
    // Note: the Spring container handles the rollback hook later,
    // the helper itself does not manually trigger compensation inside catch block
    // when sync is active.
    var keycloakAction = mock(Runnable.class);
    @SuppressWarnings("unchecked")
    var databaseAction = (Supplier<String>) mock(Supplier.class);
    var compensationAction = mock(Runnable.class);

    var dbException = new RuntimeException("DB failed");
    txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
    when(databaseAction.get()).thenThrow(dbException);

    assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
      compensationAction))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("DB failed");

    verify(compensationAction, org.mockito.Mockito.never()).run(); // Deferred until Spring Tx Rollback
  }
}
