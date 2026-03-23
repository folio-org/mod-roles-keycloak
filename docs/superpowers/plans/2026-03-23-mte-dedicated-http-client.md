# MTE Dedicated HTTP Client — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the shared-factory `TenantEntitlementsClient` registration with a dedicated, TLS-capable HTTP client that targets MTE's own configurable base URL instead of the Okapi URL.

**Architecture:** Two new files (`MteConfigurationProperties`, `MteConfiguration`) in `org.folio.roles.integration.mte.configuration` mirror the existing Keycloak config pattern. `HttpClientTlsUtils.buildHttpServiceClient` (from `folio-tls-utils:4.0.0-SNAPSHOT`, already on the classpath) creates the proxy. The old `tenantEntitlementsClient` bean in `ClientConfig` is removed. IT tests are plumbed automatically via `wiremock-url.vars` — no new test classes needed.

**Tech Stack:** Java 21, Spring Boot 4, `folio-tls-utils` (`HttpClientTlsUtils`), `TlsProperties`, Lombok, WireMock (IT tests), JUnit 5 + Mockito (unit tests).

---

## Chunk 1: New config classes + unit test + `ClientConfig` change + YAML updates

### Task 1: Create `MteConfigurationProperties`

**Files:**
- Create: `src/main/java/org/folio/roles/integration/mte/configuration/MteConfigurationProperties.java`

- [ ] **Step 1: Create the properties class**

  ```java
  package org.folio.roles.integration.mte.configuration;

  import lombok.Getter;
  import lombok.Setter;
  import org.folio.common.configuration.properties.TlsProperties;
  import org.springframework.boot.context.properties.ConfigurationProperties;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.validation.annotation.Validated;

  /**
   * Configuration properties for the mgr-tenant-entitlements HTTP client.
   */
  @Getter
  @Setter
  @Validated
  @Configuration
  @ConfigurationProperties(prefix = "application.mte")
  public class MteConfigurationProperties {

    /**
     * MTE service base URL.
     */
    private String url;

    /**
     * TLS configuration for MTE communication.
     */
    private TlsProperties tls;
  }
  ```

---

### Task 2: Create `MteConfiguration` (TDD)

**Files:**
- Create: `src/test/java/org/folio/roles/integration/mte/configuration/MteConfigurationTest.java`
- Create: `src/main/java/org/folio/roles/integration/mte/configuration/MteConfiguration.java`

- [ ] **Step 1: Write the failing unit test**

  ```java
  package org.folio.roles.integration.mte.configuration;

  import static org.assertj.core.api.Assertions.assertThat;

  import org.folio.roles.integration.mte.TenantEntitlementsClient;
  import org.folio.test.types.UnitTest;
  import org.junit.jupiter.api.Test;
  import org.springframework.web.client.RestClient;

  @UnitTest
  class MteConfigurationTest {

    @Test
    void tenantEntitlementsClient_positive_returnsProxy() {
      var props = new MteConfigurationProperties();
      props.setUrl("http://localhost:8080");

      var config = new MteConfiguration(props);
      var client = config.tenantEntitlementsClient(RestClient.builder());

      assertThat(client).isNotNull().isInstanceOf(TenantEntitlementsClient.class);
    }
  }
  ```

- [ ] **Step 2: Run the test to confirm it fails (class does not exist)**

  ```bash
  mvn test -Dtest=MteConfigurationTest -Dcheckstyle.skip=true
  ```

  Expected: compilation error — `MteConfiguration cannot be resolved to a type`

- [ ] **Step 3: Create `MteConfiguration`**

  ```java
  package org.folio.roles.integration.mte.configuration;

  import lombok.RequiredArgsConstructor;
  import org.folio.common.utils.tls.HttpClientTlsUtils;
  import org.folio.roles.integration.mte.TenantEntitlementsClient;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.web.client.RestClient;

  /**
   * Spring configuration for the mgr-tenant-entitlements HTTP client.
   *
   * <p>Creates a dedicated {@link TenantEntitlementsClient} proxy backed by a {@link RestClient}
   * pointed at {@code application.mte.url}, independent of the shared Okapi HTTP service factory.
   * TLS is configured via {@link MteConfigurationProperties#getTls()}.</p>
   */
  @Configuration
  @RequiredArgsConstructor
  public class MteConfiguration {

    private final MteConfigurationProperties properties;

    /**
     * Creates the {@link TenantEntitlementsClient} Spring {@code @HttpExchange} proxy.
     *
     * @param builder Spring auto-configured {@link RestClient.Builder} (prototype — fresh per injection)
     * @return proxy instance backed by a dedicated {@link RestClient} targeting MTE's base URL
     */
    @Bean
    public TenantEntitlementsClient tenantEntitlementsClient(RestClient.Builder builder) {
      return HttpClientTlsUtils.buildHttpServiceClient(
        builder, properties.getTls(), properties.getUrl(), TenantEntitlementsClient.class);
    }
  }
  ```

- [ ] **Step 4: Run the test to confirm it passes**

  ```bash
  mvn test -Dtest=MteConfigurationTest -Dcheckstyle.skip=true
  ```

  Expected: `BUILD SUCCESS`, 1 test passed.

- [ ] **Step 5: Commit**

  ```bash
  git add \
    src/main/java/org/folio/roles/integration/mte/configuration/MteConfigurationProperties.java \
    src/main/java/org/folio/roles/integration/mte/configuration/MteConfiguration.java \
    src/test/java/org/folio/roles/integration/mte/configuration/MteConfigurationTest.java
  git commit -m "feat: add MteConfigurationProperties and MteConfiguration for dedicated MTE HTTP client"
  ```

---

### Task 3: Remove `tenantEntitlementsClient` from `ClientConfig`

**Files:**
- Modify: `src/main/java/org/folio/roles/configuration/ClientConfig.java`

- [ ] **Step 1: Remove the `tenantEntitlementsClient` bean method and its now-unused import**

  Current file (`src/main/java/org/folio/roles/configuration/ClientConfig.java`):

  ```java
  package org.folio.roles.configuration;

  import org.folio.roles.integration.mte.TenantEntitlementsClient;
  import org.folio.roles.integration.permissions.PermissionsClient;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.web.service.invoker.HttpServiceProxyFactory;

  @Configuration
  public class ClientConfig {

    @Bean
    public PermissionsClient permissionsClient(HttpServiceProxyFactory factory) {
      return factory.createClient(PermissionsClient.class);
    }

    @Bean
    public TenantEntitlementsClient tenantEntitlementsClient(HttpServiceProxyFactory factory) {
      return factory.createClient(TenantEntitlementsClient.class);
    }
  }
  ```

  Replace with:

  ```java
  package org.folio.roles.configuration;

  import org.folio.roles.integration.permissions.PermissionsClient;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.web.service.invoker.HttpServiceProxyFactory;

  @Configuration
  public class ClientConfig {

    @Bean
    public PermissionsClient permissionsClient(HttpServiceProxyFactory factory) {
      return factory.createClient(PermissionsClient.class);
    }
  }
  ```

- [ ] **Step 2: Run all unit tests to confirm nothing is broken**

  ```bash
  mvn test -Dcheckstyle.skip=true
  ```

  Expected: `BUILD SUCCESS`. All existing unit tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/java/org/folio/roles/configuration/ClientConfig.java
  git commit -m "refactor: remove tenantEntitlementsClient from shared ClientConfig (now in MteConfiguration)"
  ```

---

### Task 4: Add `application.mte.*` to `application.yml`

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add the MTE config block**

  In `src/main/resources/application.yml`, locate the `application:` section (currently ends at line 117 with `folio-permissions:`). Append the following block **inside** `application:`, after the existing `folio-permissions` entry:

  ```yaml
    mte:
      url: ${MTE_URL:http://mgr-tenant-entitlements:8081}
      tls:
        enabled: ${MTE_CLIENT_TLS_ENABLED:false}
        trust-store-path: ${MTE_CLIENT_TLS_TRUSTSTORE_PATH:}
        trust-store-password: ${MTE_CLIENT_TLS_TRUSTSTORE_PASSWORD:}
        trust-store-type: ${MTE_CLIENT_TLS_TRUSTSTORE_TYPE:}
  ```

  The indentation is 2 spaces (matching the rest of the `application:` block). The result should look like:

  ```yaml
  application:
    environment: ${ENV:folio}
    keycloak:
      ...
    folio-permissions:
      mapping:
        source-path: ...
    mte:
      url: ${MTE_URL:http://mgr-tenant-entitlements:8081}
      tls:
        enabled: ${MTE_CLIENT_TLS_ENABLED:false}
        trust-store-path: ${MTE_CLIENT_TLS_TRUSTSTORE_PATH:}
        trust-store-password: ${MTE_CLIENT_TLS_TRUSTSTORE_PASSWORD:}
        trust-store-type: ${MTE_CLIENT_TLS_TRUSTSTORE_TYPE:}
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add src/main/resources/application.yml
  git commit -m "feat: add application.mte.* config block with MTE_URL and TLS env vars"
  ```

---

### Task 5: Update IT test resources

**Files:**
- Modify: `src/test/resources/wiremock-url.vars`
- Modify: `src/test/resources/application-it.yml`

- [ ] **Step 1: Add `application.mte.url` to `wiremock-url.vars`**

  Current content of `src/test/resources/wiremock-url.vars`:
  ```
  okapi.url
  ```

  New content:
  ```
  okapi.url
  application.mte.url
  ```

  The `WireMockExtension` reads this file at IT startup and sets each listed Spring property to the WireMock base URL. Adding `application.mte.url` means `MteConfiguration` will point at WireMock automatically — no test code changes required.

- [ ] **Step 2: Disable MTE TLS in `application-it.yml`**

  In `src/test/resources/application-it.yml`, find the `application:` section (currently at line 18). Add the MTE TLS block inside it, after the existing `moduserskc:` entry. The result should look like:

  ```yaml
  application:
    environment: it-test
    secret-store:
      ...
    keycloak:
      tls:
        enabled: true
        ...
    moduserskc:
      url: moduserskc
    mte:
      tls:
        enabled: false
    folio-permissions:
      ...
  ```

  WireMock uses plain HTTP so TLS must be disabled.

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/resources/wiremock-url.vars src/test/resources/application-it.yml
  git commit -m "test: plumb application.mte.url via wiremock-url.vars and disable MTE TLS in IT profile"
  ```

---

### Task 6: Run full verification

- [ ] **Step 1: Run the complete test suite**

  ```bash
  mvn verify
  ```

  Expected:
  - All unit tests pass (Surefire)
  - All integration tests pass (Failsafe), including `PermissionsUserEntitledOnlyIT`
  - `BUILD SUCCESS`
  - Checkstyle within the 20-violation limit

  If IT tests fail:
  - Check that `application.mte.url` is listed in `wiremock-url.vars` with exactly that spelling
  - Check that `application-it.yml` has `application.mte.tls.enabled: false`
  - Run a single IT to see the full error: `mvn verify -Dit.test=PermissionsUserEntitledOnlyIT`
