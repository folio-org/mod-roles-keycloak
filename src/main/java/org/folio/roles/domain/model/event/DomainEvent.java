package org.folio.roles.domain.model.event;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.ToString.Exclude;
import lombok.Value;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;

@Getter
@ToString
@EqualsAndHashCode
public class DomainEvent<T> {

  private final T newObject;
  private final T oldObject;
  private final DomainEventType type;
  private Long timestamp;
  @Exclude
  private FolioExecutionContext context;

  protected DomainEvent(T newObject, T oldObject, DomainEventType type) {
    validate(newObject, oldObject, type);

    this.newObject = newObject;
    this.oldObject = oldObject;

    this.type = type;
    this.timestamp = System.currentTimeMillis();
  }

  private static <T> void validate(T newObject, T oldObject, DomainEventType type) {
    if (newObject == null && oldObject == null) {
      throw new IllegalArgumentException("Both new object or old object are null");
    }

    switch (type) {
      case CREATE -> requireNonNull(newObject, "New object has to be not null");
      case UPDATE -> {
        requireNonNull(newObject, "New object has to be not null");
        requireNonNull(oldObject, "Old object has to be not null");
      }
      case DELETE -> requireNonNull(oldObject, "Old object has to be not null");
      default -> throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  public DomainEvent<T> withContext(FolioExecutionContext context) {
    this.context = FolioExecutionContextCopy.builder()
      .tenantId(context.getTenantId())
      .okapiUrl(context.getOkapiUrl())
      .token(context.getToken())
      .userId(context.getUserId())
      .requestId(context.getRequestId())
      .folioModuleMetadata(context.getFolioModuleMetadata())
      .allHeaders(new HashMap<>(emptyIfNull(context.getAllHeaders())))
      .okapiHeaders(new HashMap<>(emptyIfNull(context.getOkapiHeaders())))
      .build();

    return this;
  }

  public DomainEvent<T> withTimestamp(Long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  @Value
  @Builder
  private static final class FolioExecutionContextCopy implements FolioExecutionContext {
    private FolioModuleMetadata folioModuleMetadata;
    private Map<String, Collection<String>> allHeaders;
    private Map<String, Collection<String>> okapiHeaders;

    private String tenantId;
    private String okapiUrl;
    private String token;
    private UUID userId;
    private String requestId;
  }
}
