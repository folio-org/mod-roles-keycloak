---
feature_id: capability-event-deduplication
title: Capability Event Deduplication
updated: 2026-02-24
---

# Capability Event Deduplication

## What it does
When processing an incoming capability event, the module normalizes the derived `Capability` and `CapabilitySetDescriptor` lists so each generated `name` appears at most once. For a narrow duplicate shape (two single-endpoint capabilities for the same path with edit methods PUT/PATCH), it merges the two capabilities into one that contains both endpoints.

## Why it exists
Capability `name` is derived from permission `resource` + `action`, so multiple permissions/resources can legitimately map to the same `name`. De-duplication ensures downstream Kafka event handling sees a single item per capability/capability-set name, and (for supported PUT/PATCH edit shapes) preserves both endpoints under that single name.

## Entry point(s)
| Type | Topic | Description |
|------|-------|-------------|
| Kafka Consumer | `folio.kafka.listener.capability.topic-pattern` | Consumes capability events and normalizes derived capability lists (de-duplication + limited endpoint merge) |

### Event processing
- The de-duplication runs inside `CapabilityEventProcessor.process()` as the last step before returning a `CapabilityResultHolder`.
- Both the "old" and "new" event payloads are processed this way before they are passed to update/persistence services.

```
KafkaMessageListener (@KafkaListener)
  -> CapabilityKafkaEventHandler.handleEvent(...)
      -> CapabilityEventProcessor.process(oldValue)
      -> CapabilityEventProcessor.process(newValue)
           -> ... create capabilities + capability set descriptors
           -> toCapabilityResultHolder(...)
                -> cleanDuplicates(capabilities, Capability::getName)
                -> cleanDuplicates(capabilitySets, CapabilitySetDescriptor::getName)
```

## Business rules and constraints

### Identity used for de-duplication
- Items are considered duplicates if they share the same `name`.
- The de-duplication keys are:
  - `Capability::getName` for capabilities
  - `CapabilitySetDescriptor::getName` for capability sets

The `name` value is generated from permission-derived `resource` and `action` (not from the permission string itself):

```
capability.name = lower(resource).replaceAll(whitespace+, "_") + "." + action
```

As a result, distinct permission names can intentionally collapse to the same capability name (e.g., separate PUT and PATCH permissions for the same resource/action).

Example (resource normalization in `name`):

```
resource = "Test Resource"  action = "view"
name     = "test_resource.view"
```

### List semantics (ordering and determinism)
- The algorithm processes items in input order and builds a new result list.
- For duplicates that are not mergeable, "first occurrence wins": the first-seen item remains, later items with the same `name` are skipped.
- The output list keeps the original relative order of the first occurrences; when a merge happens the already-added item is replaced in-place (order does not change).

Consequence: if the first-seen element is missing data that a later duplicate has (for example, the first has no endpoints and the later has an endpoint), the later element is still skipped unless the strict merge rule applies.

### Duplicate handling algorithm
The same de-duplication helper is applied to both outputs:

- `capabilities = cleanDuplicates(capabilities, Capability::getName)`
- `capabilitySets = cleanDuplicates(capabilitySets, CapabilitySetDescriptor::getName)`

Mechanically, `cleanDuplicates(...)` keeps a map of `identifier -> (element, indexInResultList)`:

```
                +---------------------------+
input element ->| identifier = element.name |-> lookup in visitedIdentifiers
                +---------------------------+
                               |
               +---------------+----------------+
               |                                |
           not seen                           seen
               |                                |
               v                                v
     append to result                    attempt merge
     remember (element, idx)            (Capabilities only)
               |                                |
               v                       +-------+---------+
            next                       |                 |
                                       | merge succeeds  | merge fails
                                       v                 v
                           replace result[idx]      log INFO and drop
                           remember (merged, idx)   later duplicate
```

```
for element in inputList:
  id = identifiersMapper(element)

  if id not seen:
    append element to result
    remember (element, index)
    continue

  // duplicate id
  if mergeCapabilities(element, seenElement) succeeds:
    replace result[index] with mergedElement
    remember (mergedElement, index)
  else:
    log INFO "Duplicated <type> name found" and skip element
```

Notes:
- For non-`Capability` items (e.g., `CapabilitySetDescriptor`), merging is never attempted; duplicates are always skipped.
- Once a capability was merged, its endpoints list has 2 items; any later duplicates of the same `name` cannot be merged again (the merge rule requires exactly 1 endpoint on each side), so they are skipped.
- The de-duplication only applies to the top-level lists returned by `CapabilityEventProcessor` (`CapabilityResultHolder.capabilities()` and `.capabilitySets()`). It does not de-duplicate nested lists (for example, the `capabilities` list inside a `CapabilitySetDescriptor`).

### Supported merge case: PUT/PATCH endpoint pairs
Two duplicate `Capability` objects with the same `name` are mergeable only when all of the following are true:

- Each capability has exactly 1 endpoint (`endpoints.size() == 1` on both sides)
- Both endpoints share the same `path`
- Both endpoints have an "edit" HTTP method: `PUT` or `PATCH`

When the merge succeeds:
- The merged capability starts as a copy of the first-seen capability (properties copied from the earlier item).
- `endpoints` becomes a 2-element list containing the earlier endpoint and the later endpoint (in that order).
- `permission` is chosen to prefer the permission that ends with `.put` (if the later duplicate has a permission name ending with `.put`, it replaces the earlier permission; otherwise the earlier permission is kept).

Internally, the merge is "copy-first-then-overlay":

```
existing (first seen)                   later duplicate
  foundCapability.endpoints = [E1]        capability.endpoints = [E2]
  E1.path == E2.path
  E1.method in {PUT,PATCH}
  E2.method in {PUT,PATCH}

=> merged capability
   - copy all properties from foundCapability
   - endpoints  = [E1, E2]
   - permission = prefer a permission ending with ".put"
```

Constraint: the merge rule does not require the two endpoint methods to be different. If both are PUT (or both are PATCH) and the path matches, they still merge, producing a 2-element endpoints list with the same method twice.

Example (mergeable):

```
Input (two capabilities with the same name)
  A: permission = "foo.item.put"   endpoints = [{ path: "/foo/items/{id}", method: PUT   }]
  B: permission = "foo.item.patch" endpoints = [{ path: "/foo/items/{id}", method: PATCH }]

Output (single capability)
  permission = "foo.item.put"
  endpoints  = [PUT /foo/items/{id}, PATCH /foo/items/{id}]
```

Example (not mergeable; order matters):

```
Input (same name, but methods are not both PUT/PATCH)
  A: endpoints = [{ path: "/foo/items/{id}", method: GET }]
  B: endpoints = [{ path: "/foo/items/{id}", method: PUT }]

Output
  keep A (first occurrence), skip B, log INFO about duplicate name
```

## Error behavior (if applicable)
- Duplicate names that cannot be merged are not treated as errors: later duplicates are skipped and an INFO log entry is emitted.
- The merge is best-effort and intentionally conservative; any deviation from the supported merge shape (endpoint count not 1, path mismatch, method not PUT/PATCH) results in "no merge" and the later duplicate is skipped.

Observed log shape when a duplicate is skipped:

```
Duplicated <capability|capabilitySetDescriptor> name found: resource = <name>
```

## Configuration (if applicable)
| Variable | Purpose |
|----------|---------|
| `folio.kafka.listener.capability.topic-pattern` / `KAFKA_CAPABILITIES_TOPIC_PATTERN` | Controls which Kafka topics deliver the capability events that are subject to this de-duplication |

## Dependencies and interactions (if applicable)
- **Consumes** capability events via `KafkaMessageListener` (`@KafkaListener` topic pattern driven by `folio.kafka.listener.capability.topic-pattern`).
- **Produces** a normalized `CapabilityResultHolder` that is then used by capability update/persistence services during Kafka event handling.
