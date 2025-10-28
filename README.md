# mod-roles-keycloak

Copyright (C) 2023-2025 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for
more information.

## Table of contents

* [Introduction](#introduction)
* [Environment Variables](#environment-variables)
  * [Secure storage environment variables](#secure-storage-environment-variables)
    * [AWS-SSM](#aws-ssm)
    * [Vault](#vault)
    * [Folio Secure Store Proxy (FSSP)](#folio-secure-store-proxy-fssp)
  * [Keycloak environment variables](#keycloak-environment-variables)
* [Loading of client IDs/secrets](#loading-of-client-idssecrets)
* [Custom permission-capability mappings](#custom-permission-capability-mappings)

## Introduction

For now, `mod-roles-keycloak` proxies requests to Keycloak. Service helps manage roles and policies: creating,
updating, deleting and searching. The service can be used to associate roles with the user.`mod-roles-keycloak` stores
metadata about who and when created a record.

## Environment Variables

| Name                                  | Default value                                                                                                                                          | Required | Description                                                                                                                      |
|:--------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------|:--------:|:---------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                               | localhost                                                                                                                                              |  false   | Postgres hostname                                                                                                                |
| DB_PORT                               | 5432                                                                                                                                                   |  false   | Postgres port                                                                                                                    |
| DB_USERNAME                           | postgres                                                                                                                                               |  false   | Postgres username                                                                                                                |
| DB_PASSWORD                           | postgres                                                                                                                                               |  false   | Postgres username password                                                                                                       |
| DB_DATABASE                           | postgres                                                                                                                                               |  false   | Postgres database name                                                                                                           |
| KC_URL                                | keycloak:8080                                                                                                                                          |  false   | Keycloak URL used to perform HTTP requests by `KeycloakClient`.                                                                  |
| KC_ADMIN_CLIENT_ID                    | folio-backend-admin-client                                                                                                                             |   true   | Admin client for issuing admin tokens                                                                                            |
| KC_LOGIN_CLIENT_SUFFIX                | -login-application                                                                                                                                     |  false   | Client name suffix for storing policies in Keycloak                                                                              |
| KC_USER_ID_CACHE_TTL                  | 180s                                                                                                                                                   |  false   | Time to live in sec for cached `keycloakUserId` by folio `userId`                                                                |
| KAFKA_CAPABILITIES_TOPIC_PATTERN      | `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.capability`                                                                               |  false   | Topic pattern for `capability` topic filled by mgr-tenants-entitlement                                                           |
| CAPABILITY_TOPIC_RETRY_DELAY          | 1s                                                                                                                                                     |  false   | `capability` topic retry delay if tenant is not initialized                                                                      |
| CAPABILITY_TOPIC_RETRY_ATTEMPTS       | 9223372036854775807                                                                                                                                    |  false   | `capability` topic retry attempts if tenant is not initialized (default value is Long.MAX_VALUE ~= infinite amount of retries)   |
| FOLIO_PERMISSIONS_MAPPING_SOURCE_PATH | [folio permission mapping json file](https://raw.githubusercontent.com/folio-org/folio-permissions-mappings/refs/heads/master/mappings-overrides.json) |  false   | Link or path to resource that contains folio permission mappings. File path or URL can be used.                                  |
| CACHE_PERMISSION_MAPPINGS_TTL         | 60                                                                                                                                                     | false    | TTL for cache of permission mapping overrides, in seconds                                                                        |

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
| KC_RETRY_MAX_ATTEMPTS             | Control how many times a Keycloak request should be retried upon failure before giving up                                                                                  |
| KC_RETRY_BACKOFF_DELAY_MS         | Specify how long the application should wait before retrying a failed operation related to Keycloak integration                                                            |
| KC_RETRY_BACKOFF_MULTIPLIER       | Used to increase the delay between retry attempts exponentially                                                                                                            |

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
