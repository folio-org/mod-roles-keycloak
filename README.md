# mod-roles-keycloak

Copyright (C) 2023-2025 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for
more information.

## Table of contents

* [Introduction](#introduction)
* [Migration API](#migration-api)
* [Environment Variables](#environment-variables)
  * [Secure storage environment variables](#secure-storage-environment-variables)
    * [AWS-SSM](#aws-ssm)
    * [Vault](#vault)
    * [Folio Secure Store Proxy (FSSP)](#folio-secure-store-proxy-fssp)
  * [Keycloak environment variables](#keycloak-environment-variables)
* [Loading of client IDs/secrets](#loading-of-client-idssecrets)
* [Custom permission-capability mappings](#custom-permission-capability-mappings)
* [Capability duplicate removal](#capability-duplicate-removal)

## Introduction

For now, `mod-roles-keycloak` proxies requests to Keycloak. Service helps manage roles and policies: creating,
updating, deleting and searching. The service can be used to associate roles with the user.`mod-roles-keycloak` stores
metadata about who and when created a record.

## Migration API

The module provides asynchronous migration API to migrate legacy permissions to the new roles-based authorization model.

### Key Features

- **Asynchronous Processing**: Migration runs in the background, returning a job ID immediately
- **Progress Tracking**: Monitor migration status via `GET /roles/migrations/{jobId}`
- **Error Logging**: Detailed error tracking for failed operations with root cause analysis
- **CQL Query Support**: Search jobs and errors using CQL queries
- **Job Management**: List all jobs, get job details, and delete completed jobs

### API Endpoints

- `POST /roles/migrations` - Start a new migration job
- `GET /roles/migrations` - List all migration jobs (supports CQL queries)
- `GET /roles/migrations/{jobId}` - Get migration job details
- `DELETE /roles/migrations/{jobId}` - Delete a migration job
- `GET /roles/migrations/{jobId}/errors` - Get migration errors (supports CQL queries)

### Migration Process

1. **Role Creation**: Creates roles in both Keycloak and local database
   - Automatic rollback if database creation fails after Keycloak success
   - Detailed error logging with root cause extraction
   - Continues processing remaining roles on individual failures

2. **User Assignment**: Assigns created roles to users based on legacy permissions
   - Batch processing for optimal performance
   - Transactional to ensure data consistency

3. **Error Handling**: Comprehensive error tracking
   - Each failed role creation generates an error record
   - Error details include: error type, message, root cause, entity information
   - Query errors via REST API for debugging

### Configuration

Maximum concurrent migrations: **1** (configurable in code)

## Environment Variables

| Name                                  | Default value                                                                                                                                          | Required | Description                                                                                                                             |
|:--------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------|:--------:|:----------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                               | localhost                                                                                                                                              |  false   | Postgres hostname                                                                                                                       |
| DB_PORT                               | 5432                                                                                                                                                   |  false   | Postgres port                                                                                                                           |
| DB_USERNAME                           | postgres                                                                                                                                               |  false   | Postgres username                                                                                                                       |
| DB_PASSWORD                           | postgres                                                                                                                                               |  false   | Postgres username password                                                                                                              |
| DB_DATABASE                           | postgres                                                                                                                                               |  false   | Postgres database name                                                                                                                  |
| KC_URL                                | keycloak:8080                                                                                                                                          |  false   | Keycloak URL used to perform HTTP requests by `KeycloakClient`.                                                                         |
| KC_ADMIN_CLIENT_ID                    | folio-backend-admin-client                                                                                                                             |   true   | Admin client for issuing admin tokens                                                                                                   |
| KC_LOGIN_CLIENT_SUFFIX                | -login-application                                                                                                                                     |  false   | Client name suffix for storing policies in Keycloak                                                                                     |
| KC_USER_ID_CACHE_TTL                  | 180s                                                                                                                                                   |  false   | Time to live in sec for cached `keycloakUserId` by folio `userId`                                                                       |
| USER_PERMISSIONS_CACHE_TTL            | 30s                                                                                                                                                    |  false   | Time to live for cached user permissions. Cache is evicted on role/capability changes. Can be set to average user session length + 10%. |
| USER_PERMISSIONS_CACHE_MAX_SIZE       | 1000                                                                                                                                                   |  false   | Maximum number of cache entries. This limit is shared across all tenants. Estimate based on concurrent active users across all tenants. |
| KAFKA_CAPABILITIES_TOPIC_PATTERN      | `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.capability`                                                                               |  false   | Topic pattern for `capability` topic filled by mgr-tenants-entitlement                                                                  |
| CAPABILITY_TOPIC_RETRY_DELAY          | 1s                                                                                                                                                     |  false   | `capability` topic retry delay if tenant is not initialized                                                                             |
| CAPABILITY_TOPIC_RETRY_ATTEMPTS       | 9223372036854775807                                                                                                                                    |  false   | `capability` topic retry attempts if tenant is not initialized (default value is Long.MAX_VALUE ~= infinite amount of retries)          |
| FOLIO_PERMISSIONS_MAPPING_SOURCE_PATH | [folio permission mapping json file](https://raw.githubusercontent.com/folio-org/folio-permissions-mappings/refs/heads/master/mappings-overrides.json) |  false   | Link or path to resource that contains folio permission mappings. File path or URL can be used.                                         |
| CACHE_PERMISSION_MAPPINGS_TTL         | 60                                                                                                                                                     |  false   | TTL for cache of permission mapping overrides, in seconds                                                                               |

See also configurations from https://github.com/folio-org/folio-spring-support/tree/release/v8.1/folio-spring-system-user - FOLIO_ENVIRONMENT, FOLIO_OKAPI_URL, FOLIO_SYSTEM_USER_USERNAME, FOLIO_SYSTEM_USER_PASSWORD.

### Secure storage environment variables

| Name                | Default value | Description                                                                                                                                                    |
|:--------------------|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SECURE_STORE_ENV    | folio         | First segment of the secure store key, for example `prod` or `test`. Defaults to `folio`. In Ramsons and Sunflower defaults to ENV with fall-back `folio`.     |

#### AWS-SSM

Required when `SECRET_STORE_TYPE=AWS_SSM`

| Name                                           | Default value | Description                                                                                                                                                    |
|:-----------------------------------------------|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SECRET_STORE_AWS_SSM_REGION                    | -             | The AWS region to pass to the AWS SSM Client Builder. If not set, the AWS Default Region Provider Chain is used to determine which region to use.              |
| SECRET_STORE_AWS_SSM_USE_IAM                   | true          | If true, will rely on the current IAM role for authorization instead of explicitly providing AWS credentials (access_key/secret_key)                           |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_ENDPOINT  | -             | The HTTP endpoint to use for retrieving AWS credentials. This is ignored if useIAM is true                                                                     |
| SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_PATH      | -             | The path component of the credentials endpoint URI. This value is appended to the credentials endpoint to form the URI from which credentials can be obtained. |

#### Vault

Required when `SECRET_STORE_TYPE=VAULT`

| Name                                    | Default value | Description                                                                         |
|:----------------------------------------|:--------------|:------------------------------------------------------------------------------------|
| SECRET_STORE_VAULT_TOKEN                | -             | token for accessing vault, may be a root token                                      |
| SECRET_STORE_VAULT_ADDRESS              | -             | the address of your vault                                                           |
| SECRET_STORE_VAULT_ENABLE_SSL           | false         | whether or not to use SSL                                                           |
| SECRET_STORE_VAULT_PEM_FILE_PATH        | -             | the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding    |
| SECRET_STORE_VAULT_KEYSTORE_PASSWORD    | -             | the password used to access the JKS keystore (optional)                             |
| SECRET_STORE_VAULT_KEYSTORE_FILE_PATH   | -             | the path to a JKS keystore file containing a client cert and private key            |
| SECRET_STORE_VAULT_TRUSTSTORE_FILE_PATH | -             | the path to a JKS truststore file containing Vault server certs that can be trusted |

#### Folio Secure Store Proxy (FSSP)

Required when `SECRET_STORE_TYPE=FSSP`

| Name                                   | Default value         | Description                                          |
|:---------------------------------------|:----------------------|:-----------------------------------------------------|
| SECRET_STORE_FSSP_ADDRESS              | -                     | The address (URL) of the FSSP service.               |
| SECRET_STORE_FSSP_SECRET_PATH          | secure-store/entries  | The path in FSSP where secrets are stored/retrieved. |
| SECRET_STORE_FSSP_ENABLE_SSL           | false                 | Whether to use SSL when connecting to FSSP.          |
| SECRET_STORE_FSSP_TRUSTSTORE_PATH      | -                     | Path to the truststore file for SSL connections.     |
| SECRET_STORE_FSSP_TRUSTSTORE_FILE_TYPE | -                     | The type of the truststore file (e.g., JKS, PKCS12). |
| SECRET_STORE_FSSP_TRUSTSTORE_PASSWORD  | -                     | The password for the truststore file.                |

### Keycloak environment variables

Keycloak all configuration properties: https://www.keycloak.org/server/all-config

| Name                              | Description                                                                                                                                                                |
|:----------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KC_HOSTNAME                       | Keycloak hostname, will be added to returned endpoints, for example for openid-configuration                                                                               |
| KC_ADMIN                          | Initial admin username                                                                                                                                                     |
| KC_ADMIN_PASSWORD                 | Initial admin password                                                                                                                                                     |
| KC_DB                             | Database type                                                                                                                                                              |
| KC_DB_URL_DATABASE                | Sets the database name of the default JDBC URL of the chosen vendor. If the DB_URL option is set, this option is ignored.                                                  |
| KC_DB_URL_HOST                    | Sets the hostname of the default JDBC URL of the chosen vendor. If the DB_URL option is set, this option is ignored.                                                       |
| KC_DB_URL_PORT                    | Sets the port of the default JDBC URL of the chosen vendor. If the DB_URL option is set, this option is ignored.                                                           |
| KC_DB_USERNAME                    | Database Username                                                                                                                                                          |
| KC_DB_PASSWORD                    | Database Password                                                                                                                                                          |
| KC_PROXY                          | The proxy address forwarding mode if the server is behind a reverse proxy. Possible values are: edge, reencrypt, passthrough. https://www.keycloak.org/server/reverseproxy |
| KC_HOSTNAME_STRICT                | Disables dynamically resolving the hostname from request headers. Should always be set to true in production, unless proxy verifies the Host header.                       |
| KC_HOSTNAME_PORT                  | The port used by the proxy when exposing the hostname. Set this option if the proxy uses a port other than the default HTTP and HTTPS ports. Defaults to -1.               |
| KC_CLIENT_TLS_ENABLED             | Enables TLS for keycloak clients.                                                                                                                                          |
| KC_CLIENT_TLS_TRUSTSTORE_PATH     | Truststore file path for keycloak clients.                                                                                                                                 |
| KC_CLIENT_TLS_TRUSTSTORE_PASSWORD | Truststore password for keycloak clients.                                                                                                                                  |
| KC_CLIENT_TLS_TRUSTSTORE_TYPE     | Truststore file type for keycloak clients.                                                                                                                                 |
| KC_RETRY_MAX_ATTEMPTS             | Control how many times a Keycloak request should be retried upon failure before giving up                                                                                                                                                  |
| KC_RETRY_BACKOFF_DELAY_MS         | Specify how long the application should wait before retrying a failed operation related to Keycloak integration                                                                                                                            |
| KC_CONCURRENCY_THREAD_POOL_SIZE   | Maximum number of threads in the shared Keycloak operations thread pool. The pool grows on demand (cached behaviour) and shrinks back to zero when idle. Controls concurrency of parallel permission create/delete calls against Keycloak. Default: `20` |

## Loading of client IDs/secrets

The module pulls client_secret for client_id from AWS Parameter store, Vault or other reliable secret storages when they
are required for login. The credentials are cached for 3600s.

## Custom permission-capability mappings

In order to avoid issues resulting from mapping permissions to capabilities (such as overlapping capabilities in cases
of incorrect permission naming etc) mod-roles-keycloak provides a way  to define a custom mapping from permission
to capability - via file mappings-overrides.json, placed in folio-permissions folder (see src/main/resources).

One can define custom mapping of a module-descriptor permission to Eureka capability in this file. For example:
```
{
  "some.nonstandard.named.permission": {
    "resource": "Nonstandard entity",
    "action": "execute",
    "type": "procedural"
  },
  ...
}
```

See [Permissions naming convention](https://folio-org.atlassian.net/wiki/spaces/FOLIJET/pages/156368925/Permissions+naming+convention) for more
information regarding permission properties such as "action", permission naming conventions and other permission related
information.

## Capability duplicate removal

The application automatically removes duplicate capabilities during tenant initialization. This ensures data consistency
when the same capability exists under different names.

### What gets merged

When a duplicate capability is detected:
- All **role-to-capability** assignments are migrated from the old capability to the new one
- All **user-to-capability** assignments are migrated from the old capability to the new one
- All **role-to-capability-set** assignments are migrated from the old capability set to the new one
- All **user-to-capability-set** assignments are migrated from the old capability set to the new one
- All **loadable permission** references are updated to point to the new capability

### What gets cleaned up

After successful migration:
- The old/duplicate capability is deleted from the database
- The old/duplicate capability set is deleted from the database
- All related links to the old capability/capability set are removed

### Adding new duplicate capability pairs

To add a new capability pair for merging, edit the `CapabilitiesMergeService` class in
`src/main/java/org/folio/roles/service/migration/CapabilitiesMergeService.java`:

```java
@Transactional
public void mergeDuplicateCapabilities() {
  log.info("Starting capability duplicates merge");
  try {
    capabilityDuplicateMigrationService.migrate(
      "old_capability_name",
      "new_capability_name");
    log.info("Capability duplicates merge completed successfully");
  } catch (Exception e) {
    log.warn("Error during capability duplicates merge", e);
  }
}
```

**Parameters:**
- `old_capability_name` - Name of the deprecated/duplicate capability or capability set to be removed
- `new_capability_name` - Name of the replacement capability or capability set

If either the old or new capability does not exist, the migration is skipped with a log message.
Migration errors are logged but do not block tenant initialization.
