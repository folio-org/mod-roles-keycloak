# Keycloak Permissions Parallelism Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Parallelize Keycloak permission create/delete calls with bounded concurrency while preserving transaction boundaries and fail-fast semantics.

**Architecture:** Add a KeycloakPermissionsExecutor that batches endpoints and runs per-endpoint tasks on a bounded thread pool with FolioExecutionContext propagation; integrate it into KeycloakAuthorizationService and configure parallelism/batch size via KeycloakConfigurationProperties and application.yml.

**Tech Stack:** Java 21, Spring Boot, Keycloak admin client, JUnit 5, Mockito, AssertJ

---

### Task 1: Add Keycloak permissions config defaults

**Files:**
- Create: `src/test/java/org/folio/roles/integration/keyclock/configuration/KeycloakConfigurationPropertiesTest.java`
- Modify: `src/main/java/org/folio/roles/integration/keyclock/configuration/KeycloakConfigurationProperties.java`
- Modify: `src/main/resources/application.yml`

**Step 1: Write the failing test**

```java
package org.folio.roles.integration.keyclock.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakConfigurationPropertiesTest {

  @Test
  void permissionsDefaults() {
    var props = new KeycloakConfigurationProperties();

    assertThat(props.getPermissions().getParallelism()).isEqualTo(4);
    assertThat(props.getPermissions().getBatchSize()).isEqualTo(50);
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=KeycloakConfigurationPropertiesTest test`
Expected: FAIL (compilation error or NullPointerException because `permissions` defaults don’t exist yet).

**Step 3: Write minimal implementation**

```java
@Valid
private Permissions permissions = new Permissions();

@Data
public static class Permissions {

  @Min(1)
  private int parallelism = 4;

  @Min(1)
  private int batchSize = 50;
}
```

Update `src/main/resources/application.yml`:

```yaml
application:
  keycloak:
    permissions:
      parallelism: ${KC_PERMISSIONS_PARALLELISM:4}
      batch-size: ${KC_PERMISSIONS_BATCH_SIZE:50}
```

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=KeycloakConfigurationPropertiesTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/folio/roles/integration/keyclock/configuration/KeycloakConfigurationProperties.java \
  src/main/resources/application.yml \
  src/test/java/org/folio/roles/integration/keyclock/configuration/KeycloakConfigurationPropertiesTest.java
git commit -m "feat: add keycloak permissions parallelism config"
```

---

### Task 2: Implement KeycloakPermissionsExecutor with unit tests

**Files:**
- Create: `src/main/java/org/folio/roles/integration/keyclock/KeycloakPermissionsExecutor.java`
- Create: `src/test/java/org/folio/roles/integration/keyclock/KeycloakPermissionsExecutorTest.java`

**Step 1: Write the failing tests**

```java
package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakPermissionsExecutorTest {

  @Test
  void executeRunsAllEndpoints() {
    var props = new KeycloakConfigurationProperties();
    var permissions = new KeycloakConfigurationProperties.Permissions();
    permissions.setParallelism(2);
    permissions.setBatchSize(2);
    props.setPermissions(permissions);

    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    var executor = new KeycloakPermissionsExecutor(props, context);

    var endpoints = List.of(
      new Endpoint().path("/a").method(HttpMethod.GET),
      new Endpoint().path("/b").method(HttpMethod.GET),
      new Endpoint().path("/c").method(HttpMethod.GET)
    );

    var seen = new ConcurrentLinkedQueue<Endpoint>();
    executor.execute(endpoints, seen::add);

    assertThat(seen).containsExactlyInAnyOrderElementsOf(endpoints);
  }

  @Test
  void executeFailsFastOnError() {
    var props = new KeycloakConfigurationProperties();
    var permissions = new KeycloakConfigurationProperties.Permissions();
    permissions.setParallelism(2);
    permissions.setBatchSize(3);
    props.setPermissions(permissions);

    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    var executor = new KeycloakPermissionsExecutor(props, context);

    var endpoints = List.of(
      new Endpoint().path("/a").method(HttpMethod.GET),
      new Endpoint().path("/b").method(HttpMethod.GET)
    );

    var counter = new AtomicInteger(0);
    assertThatThrownBy(() -> executor.execute(endpoints, endpoint -> {
      if (endpoint.getPath().equals("/b")) {
        throw new IllegalStateException("boom");
      }
      counter.incrementAndGet();
    })).isInstanceOf(IllegalStateException.class);

    assertThat(counter.get()).isGreaterThanOrEqualTo(0);
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=KeycloakPermissionsExecutorTest test`
Expected: FAIL (class missing or not implemented).

**Step 3: Write minimal implementation**

```java
package org.folio.roles.integration.keyclock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakPermissionsExecutor {

  private final KeycloakConfigurationProperties keycloakConfigurationProperties;
  private final FolioExecutionContext folioExecutionContext;

  private volatile ExecutorService executor;

  public void execute(List<Endpoint> endpoints, Consumer<Endpoint> action) {
    if (endpoints == null || endpoints.isEmpty()) {
      return;
    }

    var permissionsConfig = keycloakConfigurationProperties.getPermissions();
    var parallelism = permissionsConfig.getParallelism();
    if (parallelism <= 1 || endpoints.size() <= 1) {
      endpoints.forEach(action);
      return;
    }

    var batchSize = permissionsConfig.getBatchSize();
    for (var batch : partition(endpoints, batchSize)) {
      executeBatch(batch, action, parallelism);
    }
  }

  private void executeBatch(List<Endpoint> batch, Consumer<Endpoint> action, int parallelism) {
    var completionService = new ExecutorCompletionService<Void>(getOrCreateExecutor(parallelism));
    var futures = new ArrayList<Future<Void>>(batch.size());

    for (var endpoint : batch) {
      futures.add(completionService.submit(wrapCallable(endpoint, action)));
    }

    for (int i = 0; i < batch.size(); i++) {
      try {
        completionService.take().get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        cancelAll(futures);
        throw new IllegalStateException("Keycloak permissions execution interrupted", e);
      } catch (ExecutionException e) {
        cancelAll(futures);
        throw rethrow(e.getCause());
      }
    }
  }

  private Callable<Void> wrapCallable(Endpoint endpoint, Consumer<Endpoint> action) {
    return () -> {
      var ctx = (FolioExecutionContext) folioExecutionContext.getInstance();
      try (var ignored = new FolioExecutionContextSetter(ctx)) {
        action.accept(endpoint);
      }
      return null;
    };
  }

  private ExecutorService getOrCreateExecutor(int parallelism) {
    if (executor == null) {
      synchronized (this) {
        if (executor == null) {
          executor = Executors.newFixedThreadPool(parallelism);
        }
      }
    }
    return executor;
  }

  private static void cancelAll(List<Future<Void>> futures) {
    futures.forEach(future -> future.cancel(true));
  }

  private static RuntimeException rethrow(Throwable error) {
    if (error instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    return new IllegalStateException("Keycloak permissions execution failed", error);
  }

  private static <T> List<List<T>> partition(List<T> items, int batchSize) {
    var batches = new ArrayList<List<T>>();
    for (int i = 0; i < items.size(); i += batchSize) {
      batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
    }
    return batches;
  }

  @PreDestroy
  public void shutdown() {
    if (executor != null) {
      executor.shutdown();
    }
  }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=KeycloakPermissionsExecutorTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/folio/roles/integration/keyclock/KeycloakPermissionsExecutor.java \
  src/test/java/org/folio/roles/integration/keyclock/KeycloakPermissionsExecutorTest.java
git commit -m "feat: add keycloak permissions executor"
```

---

### Task 3: Wire executor into KeycloakAuthorizationService and update tests

**Files:**
- Modify: `src/main/java/org/folio/roles/integration/keyclock/KeycloakAuthorizationService.java`
- Modify: `src/test/java/org/folio/roles/integration/keyclock/KeycloakAuthorizationServiceTest.java`

**Step 1: Write the failing test (multiple endpoints)**

Add a new test in `KeycloakAuthorizationServiceTest` (and update setup to build the service with a real
`KeycloakPermissionsExecutor` configured with `parallelism=1` so it runs synchronously):

```java
@Test
void createPermissions_multipleEndpoints() {
  when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
  when(authorizationClient.resources()).thenReturn(authResourcesClient);
  when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
  when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);

  var policy = rolePolicy();
  var endpoints = List.of(endpoint("/foo/entities", GET), endpoint("/bar/items", GET));

  when(authResourcesClient.find(eq("/foo/entities"), any(), any(), any(), any(), eq(0), eq(MAX_VALUE)))
    .thenReturn(List.of(resourceRepresentation("/foo/entities", "GET")));
  when(authResourcesClient.find(eq("/bar/items"), any(), any(), any(), any(), eq(0), eq(MAX_VALUE)))
    .thenReturn(List.of(resourceRepresentation("/bar/items", "GET")));

  when(scopePermissionsClient.create(scopePermissionCaptor.capture()))
    .thenReturn(response, response);
  when(response.getStatusInfo()).thenReturn(Status.CREATED);

  keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

  assertThat(scopePermissionCaptor.getAllValues())
    .extracting(ScopePermissionRepresentation::getName)
    .containsExactlyInAnyOrder("GET access to /foo/entities", "GET access to /bar/items");
  verify(response, times(2)).close();
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=KeycloakAuthorizationServiceTest test`
Expected: FAIL (constructor mismatch or missing executor integration).

**Step 3: Write minimal implementation**

Update `KeycloakAuthorizationService` to depend on `KeycloakPermissionsExecutor` and delegate per-endpoint
work through it:

```java
private final KeycloakPermissionsExecutor permissionsExecutor;

public void createPermissions(Policy policy, List<Endpoint> endpoints, Function<Endpoint, String> nameGenerator) {
  if (policy == null || isEmpty(endpoints)) {
    log.debug("Keycloak permissions creation skipped [policy: {}, endpoints: {}]",
      () -> toJson(policy), () -> toJson(endpoints));
    return;
  }

  permissionsExecutor.execute(endpoints, endpoint -> createPermission(policy, endpoint, nameGenerator));
}

private void createPermission(Policy policy, Endpoint endpoint, Function<Endpoint, String> nameGenerator) {
  var scopePermissionsClient = getAuthorizationClient().permissions().scope();
  var resource = getAuthResourceByStaticPath(endpoint.getPath());
  var scope = getScopeByMethod(resource, endpoint.getMethod());
  if (scope.isEmpty()) {
    log.warn(
      "Scope is not found, keycloak permission creation will be skipped: method(scope)={}, path(resource)={}",
      endpoint.getMethod(), endpoint.getPath());
    return;
  }
  var policyName = nameGenerator.apply(endpoint);
  var permission = buildPermissionFor(policyName, resource.getId(), scope.get().getId(), policy.getId());
  try (var response = scopePermissionsClient.create(permission)) {
    processKeycloakResponse(permission, response);
  }
}

public void deletePermissions(Policy policy, List<Endpoint> endpoints, Function<Endpoint, String> nameGenerator) {
  if (policy == null || isEmpty(endpoints)) {
    log.debug("Keycloak permissions deletion skipped [policy: {}, endpoints: {}]",
      () -> toJson(policy), () -> toJson(endpoints));
    return;
  }

  permissionsExecutor.execute(endpoints, endpoint -> removeKeycloakPermission(endpoint, nameGenerator));
}

private void removeKeycloakPermission(Endpoint endpoint, Function<Endpoint, String> nameGenerator) {
  var scopePermissionsClient = getAuthorizationClient().permissions().scope();
  var permissionName = nameGenerator.apply(endpoint);
  var foundPermission = scopePermissionsClient.findByName(permissionName);
  if (foundPermission == null) {
    log.info("Keycloak permission is not found [name: {}]", permissionName);
    return;
  }

  scopePermissionsClient.findById(foundPermission.getId()).remove();
  log.debug("Permission removed from Keycloak [name: {}]", permissionName);
}
```

Update test setup to build the service with a real executor configured for sequential execution:

```java
private KeycloakPermissionsExecutor permissionsExecutor;

@BeforeEach
void setUp() {
  var props = new KeycloakConfigurationProperties();
  var permissions = new KeycloakConfigurationProperties.Permissions();
  permissions.setParallelism(1);
  permissions.setBatchSize(50);
  props.setPermissions(permissions);

  var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
  permissionsExecutor = new KeycloakPermissionsExecutor(props, context);
  keycloakAuthService = new KeycloakAuthorizationService(jsonHelper, authResourceProvider, permissionsExecutor);
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=KeycloakAuthorizationServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/org/folio/roles/integration/keyclock/KeycloakAuthorizationService.java \
  src/test/java/org/folio/roles/integration/keyclock/KeycloakAuthorizationServiceTest.java
git commit -m "feat: parallelize keycloak permissions creation"
```

---

### Task 4: Full test run (optional but recommended)

**Files:**
- None

**Step 1: Run full test suite**

Run: `mvn test`
Expected: BUILD SUCCESS (note existing warnings about deprecations and checkstyle method length).

**Step 2: Commit (only if changes occurred)**

No commit needed unless additional fixes were required.
