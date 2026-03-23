# Design: Dedicated MTE HTTP Client

**Date:** 2026-03-23  
**Module:** mod-roles-keycloak  
**Topic:** Replace the shared-factory `TenantEntitlementsClient` registration with a dedicated, TLS-capable HTTP client for the mgr-tenant-entitlements (MTE) service.

---

## Context

`TenantEntitlementsClient` is currently registered through the shared `HttpServiceProxyFactory` in `ClientConfig`. That factory is backed by the Okapi-aware `RestClient` (via `EnrichUrlAndHeadersInterceptor`), which prepends `FolioExecutionContext.getOkapiUrl()` to every request. MTE has its own base URL (not Okapi) and needs its own TLS settings independent of Okapi. This design gives MTE a self-contained, configurable client.

---

## Goal

- MTE base URL and TLS settings are driven by environment variables.
- The MTE `RestClient` is built independently of the shared Okapi `HttpServiceProxyFactory`.
- The pattern mirrors the existing Keycloak configuration (`MteConfigurationProperties` ↔ `KeycloakConfigurationProperties`, `MteConfiguration` ↔ `KeycloakConfiguration`).
- No changes to `TenantEntitlementsClient` interface or `MteEntitlementService`.

---

## New Files

### `MteConfigurationProperties.java`

**Package:** `org.folio.roles.integration.mte.configuration`

Mirrors `KeycloakConfigurationProperties` in `integration/keyclock/configuration/`.

```java
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "application.mte")
public class MteConfigurationProperties {

    /** MTE service base URL. */
    private String url;

    /** TLS configuration for MTE communication. */
    private TlsProperties tls;
}
```

- Prefix: `application.mte`
- `TlsProperties` from `org.folio.common.configuration.properties.TlsProperties` (already used by Keycloak config).

### `MteConfiguration.java`

**Package:** `org.folio.roles.integration.mte.configuration`

Mirrors `KeycloakConfiguration` in `integration/keyclock/configuration/`.

```java
@Configuration
@RequiredArgsConstructor
public class MteConfiguration {

    private final MteConfigurationProperties properties;

    @Bean
    public TenantEntitlementsClient tenantEntitlementsClient(RestClient.Builder builder) {
        return HttpClientTlsUtils.buildHttpServiceClient(
            builder, properties.getTls(), properties.getUrl(), TenantEntitlementsClient.class);
    }
}
```

- Uses `org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient` from `folio-tls-utils:4.0.0-SNAPSHOT` (already a compile dependency).
- `RestClient.Builder` is injected as the standard Spring auto-configured builder.
- The resulting client does NOT use `EnrichUrlAndHeadersInterceptor` — it targets MTE's base URL directly.

---

## Changed Files

### `ClientConfig.java`

Remove the `tenantEntitlementsClient` bean method. `TenantEntitlementsClient` is now provided by `MteConfiguration`. `PermissionsClient` registration stays unchanged.

### `application.yml`

Add under `application:`:

```yaml
mte:
  url: ${MTE_URL:http://mgr-tenant-entitlements:8081}
  tls:
    enabled: ${MTE_CLIENT_TLS_ENABLED:false}
    trust-store-path: ${MTE_CLIENT_TLS_TRUSTSTORE_PATH:}
    trust-store-password: ${MTE_CLIENT_TLS_TRUSTSTORE_PASSWORD:}
    trust-store-type: ${MTE_CLIENT_TLS_TRUSTSTORE_TYPE:}
```

Environment variable mapping:

| Env var | Property | Default |
|---|---|---|
| `MTE_URL` | `application.mte.url` | `http://mgr-tenant-entitlements:8081` |
| `MTE_CLIENT_TLS_ENABLED` | `application.mte.tls.enabled` | `false` |
| `MTE_CLIENT_TLS_TRUSTSTORE_PATH` | `application.mte.tls.trust-store-path` | _(empty)_ |
| `MTE_CLIENT_TLS_TRUSTSTORE_PASSWORD` | `application.mte.tls.trust-store-password` | _(empty)_ |
| `MTE_CLIENT_TLS_TRUSTSTORE_TYPE` | `application.mte.tls.trust-store-type` | _(empty)_ |

### `src/test/resources/wiremock-url.vars`

Add `application.mte.url` on a new line. The `WireMockExtension` reads this file and sets listed Spring properties to the WireMock base URL at IT startup. No code changes required.

### `src/test/resources/application-it.yml`

Add under `application:`:

```yaml
mte:
  tls:
    enabled: false
```

WireMock listens on plain HTTP, so TLS must be disabled in the IT profile.

---

## Unchanged

- `TenantEntitlementsClient` — interface signature stays the same (explicit `x-okapi-token` / `x-okapi-tenant` header parameters).
- `MteEntitlementService` — injects `TenantEntitlementsClient` by type; Spring finds the bean from `MteConfiguration`.
- All existing WireMock stubs under `src/test/resources/wiremock/stubs/mte/`.
- All existing integration test classes — they continue to work via the `wiremock-url.vars` injection mechanism.

---

## Data Flow (After Change)

```
HTTP request
  → MteEntitlementService
      → TenantEntitlementsClient (Spring @HttpExchange proxy)
          → RestClient (built by HttpClientTlsUtils, base URL = application.mte.url)
              → mgr-tenant-entitlements  [direct, not via Okapi]
```

Okapi headers (`x-okapi-token`, `x-okapi-tenant`) are passed explicitly as method parameters — unchanged from current behavior.

---

## Testing

No new test classes are required. The existing IT tests exercise the full call path:

1. `wiremock-url.vars` injects the WireMock URL into `application.mte.url` at IT startup.
2. `application-it.yml` sets `application.mte.tls.enabled=false`.
3. `MteConfiguration` builds the client pointing at WireMock.
4. Existing stubs (`entitled-applications-default.json`, `entitled-applications-empty.json`) serve responses as before.

---

## Risks / Notes

- `HttpClientTlsUtils.buildHttpServiceClient` creates a new `RestClient` per call. Since `MteConfiguration` is a Spring `@Configuration` singleton, the client is instantiated once at startup — no performance concern.
- The `RestClient.Builder` Spring auto-configures is a prototype bean; each injection gets a fresh builder, which is the intended usage.
