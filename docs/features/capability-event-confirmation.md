---
feature_id: capability-event-confirmation
title: Capability Event Confirmation
updated: 2026-09-10
---

# Capability Event Confirmation

## What it does

After processing each incoming capability event, the module publishes a `ResourceResultEvent` to a
dedicated Kafka topic to report the outcome back to `mgr-tenant-entitlements`. A SUCCESS result is
published when processing completes normally. A FAILURE result — including an exception detail
string — is published when all retry attempts are exhausted.

## Why it exists

Capability events are processed asynchronously. `mgr-tenant-entitlements` orchestrates entitlement
workflows across multiple modules and needs a signal to know whether each module finished its
portion successfully or failed definitively. Without this feedback loop there is no way for the
orchestrator to mark an entitlement step as done or to surface a processing error to the operator.

## Entry point(s)

**Kafka consumer (trigger)**

The confirmation is emitted as a side effect of the existing capability event consumer:

| Topic pattern                                                            | Event types                  | Listener method                              |
|--------------------------------------------------------------------------|------------------------------|----------------------------------------------|
| `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.capability` | `CREATE`, `UPDATE`, `DELETE` | `KafkaMessageListener#handleCapabilityEvent` |

**Kafka producer (output)**

| Topic                                                    | Event type           | Published by                                   |
|----------------------------------------------------------|----------------------|------------------------------------------------|
| `${EVENT_CONFIRMATION_TOPIC}` (see Configuration below) | `ResourceResultEvent`| `ResourceResultEventPublisher` (SUCCESS path)  |
| same topic                                               | `ResourceResultEvent`| `ResourceResultEventPublishingRecoverer` (FAILURE path) |

The `ResourceResultEvent` payload carries `tenant`, `resourceName` (taken from the source
`ResourceEvent`, e.g. `"Capability"`), `moduleId` (extracted from the event payload), `status`
(`SUCCESS` or `FAILURE`), and, on failure, a `details` string containing the exception message.

## Business rules and constraints

- The feature is **opt-in**. When `EVENT_CONFIRMATION_ENABLED` is `false` (the default), no
  confirmation events are produced and no Kafka producer is started.
- `moduleId` is extracted from the event payload by `CapabilityEventModuleIdExtractor`, which
  deserializes `newValue` (falling back to `oldValue` when `newValue` is absent) to `CapabilityEvent`
  and reads `getModuleId()`. If deserialization fails, `null` is sent as `moduleId` and a log entry
  at INFO level is written.
- SUCCESS is published only when `executeSystemUserScoped` completes without throwing — i.e., after
  capability processing and any replacements processing both succeed. A mid-processing exception
  prevents the SUCCESS publication.
- FAILURE is published by `capabilityRecoverer` when the `DefaultErrorHandler` calls the recoverer
  after retries are exhausted. `capabilityRecoverer` first calls `LoggingRecoverer` (writes a
  warning), then `ResourceResultEventPublishingRecoverer` (publishes the FAILURE event).
- Non-retryable exceptions (`RuntimeException` not covered by the retry back-off rules) reach the
  recoverer immediately with `FixedBackOff(0, 0)` and also produce a FAILURE confirmation.

## Error behavior

| Condition                                                     | Confirmation published |
|---------------------------------------------------------------|------------------------|
| Processing succeeds                                           | SUCCESS                |
| Processing fails, retries exhausted                           | FAILURE (with details) |
| Processing fails, non-retryable exception                     | FAILURE (with details) |
| `moduleId` extraction fails (malformed payload)               | SUCCESS or FAILURE as above, `moduleId` field is `null` |
| `EVENT_CONFIRMATION_ENABLED=false`                            | None — producer not active |

## Configuration

| Variable                    | Default                                                          | Required | Purpose                                                         |
|-----------------------------|------------------------------------------------------------------|----------|-----------------------------------------------------------------|
| `EVENT_CONFIRMATION_ENABLED` | `false`                                                         | No       | Activates the feedback loop; no confirmations are sent when `false` |
| `EVENT_CONFIRMATION_TOPIC`  | `${ENV}.mgr-tenant-entitlements.resource-result`                 | No       | Kafka topic to which `ResourceResultEvent` messages are published |

The Kafka producer uses `acks=all`, idempotent delivery (`enable.idempotence=true`), and up to 5
in-flight requests per connection with 5 retries. Type headers are suppressed
(`spring.json.add.type.headers=false`). The success listener is non-transactional
(`success-listener.transactional=false`).

## Dependencies and interactions

- **`folio-kafka-consumer` library** — supplies `ResourceResultEventPublisher` (SUCCESS path),
  `ResourceResultEventPublishingRecoverer` (FAILURE path), `LoggingRecoverer`, and
  `ModuleIdExtractor`.
- **`CapabilityEventModuleIdExtractor`** — resolves the `moduleId` field from the incoming
  `ResourceEvent` payload for inclusion in the confirmation event.
- **`KafkaConfiguration`** — wires `capabilityRecoverer` (composed from `LoggingRecoverer` and
  `ResourceResultEventPublishingRecoverer`) into `kafkaListenerContainerFactory` as the
  `DefaultErrorHandler` recoverer, replacing the previous inline warning-only log.
- **`mgr-tenant-entitlements`** — the intended consumer of the published `ResourceResultEvent`
  messages; uses them to track per-module entitlement processing outcomes.
