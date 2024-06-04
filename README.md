# mod-roles-keycloak

Copyright (C) 2023-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for
more information.

## Table of contents

* [Introduction](#introduction)
* [Environment Variables](#environment-variables)

## Introduction

For now, `mod-roles-keycloak` proxies requests to Keycloak. Service helps manage roles and policies: creating,
updating, deleting and searching. The service can be used to associate roles with the user.`mod-roles-keycloak` stores
metadata about who and when created a record.

## Environment Variables

| Name                             | Default value                                                            | Required | Description                                                                                                                            |
|:---------------------------------|:-------------------------------------------------------------------------|:--------:|:---------------------------------------------------------------------------------------------------------------------------------------|
| DB_HOST                          | localhost                                                                |  false   | Postgres hostname                                                                                                                      |
| DB_PORT                          | 5432                                                                     |  false   | Postgres port                                                                                                                          |
| DB_USERNAME                      | postgres                                                                 |  false   | Postgres username                                                                                                                      |
| DB_PASSWORD                      | postgres                                                                 |  false   | Postgres username password                                                                                                             |
| DB_DATABASE                      | postgres                                                                 |  false   | Postgres database name                                                                                                                 |
| KC_URL                           | keycloak:8080                                                            |  false   | Keycloak URL used to perform HTTP requests by `KeycloakClient`.                                                                        |
| KC_ADMIN_CLIENT_ID               | folio-backend-admin-client                                               |   true   | Admin client for issuing admin tokens                                                                                                  |
| KC_LOGIN_CLIENT_SUFFIX           | -login-application                                                       |  false   | Client name suffix for storing policies in Keycloak                                                                                    |
| KC_ADMIN_TOKEN_TTL               | 59s                                                                      |  false   | Time to live in sec for access token cache                                                                                             |
| KC_CLIENT_ID_TTL                 | 3600s                                                                    |  false   | Time to live in sec for cached client ID founded by client name                                                                        |
| KC_USER_ID_CACHE_TTL             | 10s                                                                      |  false   | Time to live in sec for cached `keycloakUserId` by folio `userId`                                                                      |
| KAFKA_CAPABILITIES_TOPIC_PATTERN | `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.capability` |  false   | Topic pattern for `capability` topic filled by mgr-tenants-entitlement                                                                 |
| CAPABILITY_TOPIC_RETRY_DELAY     | 1s                                                                       |  false   | `capability` topic retry delay if tenant is not initialized                                                                            |
| CAPABILITY_TOPIC_RETRY_ATTEMPTS  | 9223372036854775807                                                      |  false   | `capability` topic retry attempts if tenant is not initialized (default value is Long.MAX_VALUE ~= infinite amount of retries)         |
| TENANT_SERVICE_FORCE_PURGE       | false                                                                    |  false   | Forcing removal of tenant data from DB during tenant disabling. By default the data is retained in DB, ignoring `purge=true` parameter |

### Secure storage environment variables

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

## Loading of client IDs/secrets

The module pulls client_secret for client_id from AWS Parameter store, Vault or other reliable secret storages when they
are required for login. The credentials are cached for 3600s.
